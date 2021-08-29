package ctrmap.pokescript.stage0.content;

import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.stage0.EffectiveLine;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.TypeDef;
import ctrmap.pokescript.types.declarers.DeclarerController;
import ctrmap.stdlib.text.StringEx;
import ctrmap.stdlib.util.ArraysEx;
import ctrmap.stdlib.util.ParsingUtils;
import java.util.ArrayList;
import java.util.List;

public class EnumConstantDeclarationContent extends AbstractContent {

	public List<EnumConstant> constants = new ArrayList<>();

	public EnumConstantDeclarationContent(EffectiveLine l, EffectiveLine.AnalysisState state) {
		super(l);
		String[] constants = StringEx.splitOnecharFast(l.getUnterminatedData(), LangConstants.CH_ELEMENT_SEPARATOR);
		int ordinal = 0;

		int line = l.startingLine;
		
		for (String enmConstant : constants) {
			int start = StringEx.indexOfFirstNonWhitespace(enmConstant, 0);
			int linesBefore = start == -1 ? 0 : StringEx.numberOfChar(enmConstant.substring(0, start), '\n');
			line += linesBefore;
			
			int initValue = ordinal;
			int asgn = enmConstant.indexOf(LangConstants.CH_ASSIGNMENT);
			if (asgn != -1) {
				try {
					initValue = ParsingUtils.parseBasedInt(enmConstant.substring(asgn + 1).trim());
				} catch (NumberFormatException ex) {
					l.throwException("Enum constant value must be a constant integer. (" + enmConstant.substring(asgn + 1).trim() + ")");
				}
			}
			String name = enmConstant.substring(0, asgn == -1 ? enmConstant.length() : asgn).trim();
			if (name.isEmpty()) {
				continue;
			}
			if (name.contains(" ")) {
				l.throwException("Too many enum constant parameters - " + name);
			}

			EnumConstant c = new EnumConstant();
			c.name = name;
			c.ordinal = initValue;
			c.line = line;
			this.constants.add(c);

			ordinal = initValue + 1;
			
			line += StringEx.numberOfChar(enmConstant, '\n') - linesBefore;
		}
	}

	@Override
	public void addToGraph(NCompileGraph graph) {

	}

	@Override
	public void declareToGraph(NCompileGraph graph, DeclarerController declarer) {
		if (declarer.classDecl != null) {
			boolean isBitflags = declarer.classDecl.def.hasModifier(Modifier.BITFLAG);

			for (EnumConstant c : constants) {
				if (isBitflags) {
					if (c.ordinal >= Integer.SIZE) {
						line.throwException("A bitflag enum can only have values between 0 and 31!");
					} else {
						c.ordinal = (1 << c.ordinal);
					}
				}
				c.name = LangConstants.makePath(declarer.classDecl.def.className, c.name);
				//System.out.println(c.name + ": " + c.ordinal);
				c.addToGraph(declarer.classDecl.def.getTypeDef(), graph);
			}
		} else {
			throw new RuntimeException("Can not declare enum constants outside of an enum.");
		}
	}

	@Override
	public CompilerContentType getContentType() {
		return CompilerContentType.DECLARATION_ENMCONST;
	}

	public static class EnumConstant {

		public String name;
		public TypeDef type;
		public int ordinal;
		
		public int line;

		public void addToGraph(TypeDef td, NCompileGraph graph) {
			type = td;
			Variable.Global glb = new Variable.Global(name, ArraysEx.asList(Modifier.STATIC, Modifier.FINAL, Modifier.VARIABLE), td, graph, ordinal);
			graph.addGlobal(glb);
		}
	}
}
