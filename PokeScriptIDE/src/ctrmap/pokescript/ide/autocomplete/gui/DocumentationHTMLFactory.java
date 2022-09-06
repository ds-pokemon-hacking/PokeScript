package ctrmap.pokescript.ide.autocomplete.gui;

import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage0.NMember;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DocumentationHTMLFactory {

	/*
	<html>
	<body style='width: 280px'>
	<h3>DefaultUIResource</h3>
	DefaultUIResource is an HTML documentation sample. This JLabel also supports word wrapping as seen here.
	<br/>
	<br/>
	RESOUCE_NAME_VALUE = <b>0x00</b>
	<h4>Parameters:</h4>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<i>param1</i>&nbsp;&nbsp;Description of param1
	<br/>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<i>param2</i>&nbsp;&nbsp;Description of param2
	<h4>Returns:</h4>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The stuff this thingy returns.
	<h4>Throws:</h4>
	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<i>ExceptionName</i>&nbsp;&nbsp;If bad stuff happens.
	</body>
	</html>
	 */
	private static final String HTML_DOC_TAG = "html";
	private static final String HTML_BODY_TAG = "body";
	private static final String HTML_H3_TAG = "h3";
	private static final String HTML_H4_TAG = "h4";
	private static final String HTML_LINEBREAK_TAG = "br";
	private static final String HTML_NBSP_ENTITY = "nbsp";
	private static final String HTML_ITALICS_FORMAT = "i";
	private static final String HTML_BOLD_FORMAT = "b";
	private static final String HTML_LIST = "ul";
	private static final String HTML_LIST_ELEM = "li";

	public static String createDocHTML(NMember m) {
		if (m.doc != null) {
			HTMLStringBuilder sb = new HTMLStringBuilder();
			sb.appendTag(HTML_DOC_TAG);
			sb.appendTag(HTML_BODY_TAG, "style='width: 350px'", false);
			sb.append(m.type.getClassName());
			sb.append(" ");
			sb.appendTag(HTML_BOLD_FORMAT);
			sb.append(m.name);
			sb.appendTermTag(HTML_BOLD_FORMAT);
			if (!m.hasModifier(Modifier.VARIABLE)) {
				sb.append("(");
				for (int i = 0; i < m.args.length; i++) {
					sb.append(m.args[i].typeDef.getClassName());
					sb.append(" ");
					sb.appendTag(HTML_BOLD_FORMAT);
					sb.append(m.args[i].name);
					sb.appendTermTag(HTML_BOLD_FORMAT);
					if (i != m.args.length - 1) {
						sb.append(", ");
					}
				}
				sb.append(")");
			}
			sb.appendUnpairedTag(HTML_LINEBREAK_TAG);
			sb.appendUnpairedTag(HTML_LINEBREAK_TAG);
			sb.append(m.doc.header.replace("\n", "<br/>"));

			if (!m.hasModifier(Modifier.VARIABLE)) {
				if (!(m.doc.params.isEmpty() && m.doc.returns.isEmpty() && m.doc.exceptions.isEmpty())) {
					sb.appendUnpairedTag(HTML_LINEBREAK_TAG);
					sb.appendUnpairedTag(HTML_LINEBREAK_TAG);
					sb.appendDefaultEntrySet(m.doc.params.entrySet(), "Parameters");
					sb.appendDefaultList(m.doc.returns, "Returns");
					sb.appendDefaultEntrySet(m.doc.exceptions.entrySet(), "Throws");
				}
			}

			sb.appendTermTag(HTML_BODY_TAG);
			sb.appendTermTag(HTML_DOC_TAG);
			return sb.toString();
		}
		return null;
	}

	public static String createArgHtml(NMember m, int selectedArg) {
		if (m.args.length == 0) {
			return null;
			//No text at all
		}
		HTMLStringBuilder sb = new HTMLStringBuilder();

		sb.appendTag(HTML_DOC_TAG);
		sb.appendTag(HTML_BODY_TAG);

		for (int i = 0; i < m.args.length; i++) {
			if (i == selectedArg) {
				sb.appendTag(HTML_BOLD_FORMAT);
			}

			sb.append(m.args[i].typeDef.getClassName());
			sb.append(" ");
			sb.append(m.args[i].name);

			if (i != m.args.length - 1) {
				sb.append(", ");
			}

			if (i == selectedArg) {
				sb.appendTermTag(HTML_BOLD_FORMAT);
			}
		}

		sb.appendTermTag(HTML_BODY_TAG);
		sb.appendTermTag(HTML_DOC_TAG);
		return sb.toString();
	}

	public static class HTMLStringBuilder {

		public StringBuilder sb;

		public HTMLStringBuilder() {
			sb = new StringBuilder();
		}

		public void appendDefaultEntrySet(Set<Map.Entry<String, String>> set, String setName) {
			if (!set.isEmpty()) {
				appendH4Title(setName);
				for (Map.Entry<String, String> e : set) {
					appendNameAndValue(e.getKey(), e.getValue());
				}
			}
		}

		public void appendDefaultList(List<String> list, String listName) {
			if (!list.isEmpty()) {
				appendH4Title(listName);
				for (String s : list) {
					appendNameAndValue(null, s);
				}
			}
		}

		private void appendH4Title(String title) {
			appendTag(HTML_H4_TAG);
			append(title);
			append(":");
			appendTermTag(HTML_H4_TAG);
		}

		private void appendNameAndValue(String name, String value) {
			appendNbspTimes(8);
			if (name != null) {
				appendTag(HTML_BOLD_FORMAT);
				append(name);
				appendTermTag(HTML_BOLD_FORMAT);
				appendNbspTimes(2);
			}
			append(value);
			appendUnpairedTag(HTML_LINEBREAK_TAG);
		}

		private void appendNbspTimes(int times) {
			for (int i = 0; i < times; i++) {
				appendEntity(HTML_NBSP_ENTITY);
			}
		}

		public void appendTag(String tag) {
			appendTag(tag, null, false);
		}

		public void appendTermTag(String tag) {
			appendTag(tag, null, true);
		}

		public void appendTag(String tag, String params, boolean terminate) {
			append("<");
			if (terminate) {
				append("/");
			}
			append(tag);
			if (params != null) {
				append(" ");
				append(params);
			}
			append(">");
		}

		public void appendUnpairedTag(String tag) {
			append("<");
			append(tag);
			append("/");
			append(">");
		}

		public void appendEntity(String entity) {
			append("&");
			append(entity);
			append(";");
		}

		public void append(String text) {
			sb.append(text);
		}

		@Override
		public String toString() {
			return sb.toString();
		}
	}
}
