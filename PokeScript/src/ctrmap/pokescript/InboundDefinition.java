package ctrmap.pokescript;

import ctrmap.pokescript.types.DataType;
import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.stage0.CompilerAnnotation;
import ctrmap.pokescript.stage0.IModifiable;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage0.content.DeclarationContent;
import ctrmap.pokescript.types.TypeDef;
import xstandard.util.ArraysEx;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InboundDefinition implements IModifiable {

	public String name;
	public String extendsBase;

	public List<String> aliases = new ArrayList<>();
	public List<Modifier> modifiers = new ArrayList<>();
	public List<CompilerAnnotation> annotations = new ArrayList<>();
	public DeclarationContent.Argument[] args;
	public TypeDef retnType;

	public final boolean isResident;
	
	public static InboundDefinition makeResidentNative(String name, DeclarationContent.Argument[] args, TypeDef retnType) {
		return new InboundDefinition(name, args, retnType, ArraysEx.asList(Modifier.NATIVE, Modifier.STATIC), true);
	}

	public InboundDefinition() {
		isResident = false;
	}
	
	public InboundDefinition(String name, DeclarationContent.Argument[] args, TypeDef retnType, List<Modifier> modifiers) {
		this(name, args, retnType, modifiers, false);
	}
	
	public InboundDefinition(String name, DeclarationContent.Argument[] args, TypeDef retnType, List<Modifier> modifiers, boolean isResident) {
		this.name = name;
		this.args = args;
		this.retnType = retnType;
		this.modifiers = modifiers;
		this.isResident = isResident;
	}
	
	public String getNameWithoutNamespace() {
		return LangConstants.getLastPathElem(name);
	}

	public OutboundDefinition createBlankOutbound() {
		OutboundDefinition od = new OutboundDefinition(name, new Throughput[args.length]);
		for (int i = 0; i < args.length; i++) {
			od.args[i] = new Throughput(args[i].typeDef, new ArrayList<>());
		}
		return od;
	}
	
	public OutboundDefinition createOutbound(Throughput... args) {
		OutboundDefinition od = new OutboundDefinition(name, new Throughput[args.length]);
		od.args = args;
		return od;
	}

	public boolean hasAnnotation(String name) {
		for (CompilerAnnotation a : annotations) {
			if (a.name.equals(name)) {
				return true;
			}
		}
		return false;
	}

	public CompilerAnnotation getAnnotation(String name) {
		for (CompilerAnnotation a : annotations) {
			if (a.name.equals(name)) {
				return a;
			}
		}
		return null;
	}

	public List<CompilerAnnotation> getAnnotations(String name) {
		List<CompilerAnnotation> l = new ArrayList<>();
		for (CompilerAnnotation a : annotations) {
			if (a.name.equals(name)) {
				l.add(a);
			}
		}
		return l;
	}

	@Override
	public List<Modifier> getModifiers() {
		return modifiers;
	}

	public boolean hasName(String name) {
		return name.equals(this.name) || aliases.contains(name);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(name);
		sb.append("(");
		if (args.length > 0) {
			for (DeclarationContent.Argument t : args) {
				sb.append(t.typeDef.getClassName());
				sb.append(" ");
				sb.append(t.name);
				sb.append(", ");
			}
			sb.delete(sb.length() - 2, sb.length());
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o != null && o instanceof InboundDefinition) {
			InboundDefinition i = (InboundDefinition) o;
			boolean namecheck = i.name.equals(name) || i.aliases.contains(name);
			if (!namecheck) {
				for (String alias : aliases) {
					namecheck = i.name.equals(alias) || i.aliases.contains(alias);
					if (namecheck) {
						break;
					}
				}
			}
			return namecheck && i.retnType == retnType && Arrays.equals(i.args, args);
		}
		return false;
	}

	public boolean checkNameMatchesWithoutNamespace(InboundDefinition i) {
		return i.name.substring(i.name.lastIndexOf(".") + 1).equals(name.substring(name.lastIndexOf(".") + 1))
			&& i.retnType == retnType && Arrays.equals(i.args, args);
	}

	public boolean accepts(OutboundDefinition out) {
		return accepts(out, false);
	}
	
	public boolean accepts(OutboundDefinition out, boolean dirty) {
		if (out == null || !(out.name.equals(name) || aliases.contains(out.name)) || args.length != out.args.length) {
			return false;
		}
		
		if (dirty) {
			return true;
		}

		for (int i = 0; i < out.args.length; i++) {
			if (out.args == null || out.args[i] == null) {
				return false;
			}
			if (args[i].requestedModifiers.contains(Modifier.FINAL)) {
				if (!out.args[i].isImmediate()) {
					return false;
				}
			}
			TypeDef incomingType = out.args[i].type;
			TypeDef reqType = args[i].typeDef;
			if (!reqType.acceptsIncoming(incomingType)) {
				return false;
			}
		}
		return true;
	}
}
