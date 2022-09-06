package ctrmap.pokescript.data;

import ctrmap.pokescript.LangConstants;
import ctrmap.pokescript.expr.Throughput;
import ctrmap.pokescript.expr.ast.AST;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage0.CompilerAnnotation;
import ctrmap.pokescript.stage0.IModifiable;
import ctrmap.pokescript.stage0.Modifier;
import ctrmap.pokescript.stage1.NCompileGraph;
import ctrmap.pokescript.types.TypeDef;
import java.util.ArrayList;
import java.util.List;

public abstract class Variable implements IModifiable {

	public final DataGraph parent;
	
	public String name;
	public List<String> aliases = new ArrayList<>();
	public List<Modifier> modifiers = new ArrayList<>();
	public TypeDef typeDef;
	public int index;

	public abstract VarLoc getLocation();

	public abstract AInstruction getReadIns(NCompileGraph cg);

	public abstract AInstruction getWriteIns(NCompileGraph cg);

	public Variable(String name, List<Modifier> modifiers, TypeDef type, NCompileGraph cg, DataGraph parent) {
		this.name = name;
		this.modifiers = modifiers;
		this.typeDef = type;
		this.parent = parent;
		//System.out.println(typeDef);
	}

	public boolean hasName(String name) {
		return this.name.equals(name) || aliases.contains(name);
	}

	@Override
	public List<Modifier> getModifiers() {
		return modifiers;
	}
	
	public String getNameWithoutNamespace() {
		return LangConstants.getLastPathElem(name);
	}

	public void setNumeric(int n) {
		index = n;
	}

	public int getSizeOf(NCompileGraph cg) {
		//Classes will always be stored on heap
		return 1;
	}

	@Override
	public String toString() {
		return typeDef + " " + name;
	}

	public enum VarLoc {
		STACK,
		STACK_UNDER,
		DATA
	}

	public static class Global extends Variable {

		public List<AInstruction> init_from = new ArrayList<>();
		public List<CompilerAnnotation> annotations = new ArrayList<>();

		public Global(String name, List<Modifier> modifiers, TypeDef type, AST init_from, NCompileGraph cg) {
			super(name, modifiers, type, cg, cg.globals);
			if (init_from != null) {
				Throughput iptp = init_from.toThroughput();
				if (iptp != null) {
					this.init_from.addAll(iptp.getCode(type));
					optimizeInitFrom(cg);
				}
			}
		}

		public Global(String name, List<Modifier> modifiers, TypeDef type, NCompileGraph cg, int value) {
			super(name, modifiers, type, cg, cg.globals);
			init_from.add(cg.getPlain(APlainOpCode.CONST_PRI, value));
		}

		public final void optimizeInitFrom(NCompileGraph cg) {
			if (init_from.size() == 2) {
				AInstruction p_ins0 = init_from.get(0);
				AInstruction p_ins1 = init_from.get(1);
				if (p_ins0 instanceof APlainInstruction && p_ins1 instanceof APlainInstruction) {
					APlainInstruction ins0 = (APlainInstruction) p_ins0;
					APlainInstruction ins1 = (APlainInstruction) p_ins1;
					if (ins0.opCode == APlainOpCode.CONST_PRI && ins1.opCode == APlainOpCode.NEGATE) {
						init_from.clear();
						init_from.add(cg.getPlain(APlainOpCode.CONST_PRI, -ins0.getArgument(0)));
					}
				}
			}
		}

		public boolean isImmediate() {
			if (init_from.isEmpty()) {
				return true; //uninitialized
			}
			if (init_from.size() == 1) {
				AInstruction ins = init_from.get(0);
				if (ins instanceof APlainInstruction) {
					APlainOpCode cmd = ((APlainInstruction) ins).opCode;
					if (cmd == APlainOpCode.CONST_PRI) {
						return true;
					}
				}
			}
			return false;
		}

		public int getImmediateValue() {
			if (isImmediate()) {
				return ((APlainInstruction) init_from.get(0)).getArgument(0);
			}
			return 0;
		}

		@Override
		public VarLoc getLocation() {
			return VarLoc.DATA;
		}

		@Override
		public AInstruction getReadIns(NCompileGraph cg) {
			if (hasModifier(Modifier.FINAL) && isImmediate()) {
				return cg.getPlain(APlainOpCode.CONST_PRI, getImmediateValue());
			}
			return cg.getVarRead(this);
		}

		@Override
		public AInstruction getWriteIns(NCompileGraph cg) {
			return cg.getVarWrite(this);
		}
	}

	public static class Local extends Variable {

		public Local(String name, List<Modifier> modifiers, TypeDef type, NCompileGraph cg, LocalDataGraph parent) {
			super(name, modifiers, type, cg, parent);
		}

		@Override
		public VarLoc getLocation() {
			return VarLoc.STACK;
		}

		@Override
		public AInstruction getReadIns(NCompileGraph cg) {
			return cg.getVarRead(this);
		}

		@Override
		public AInstruction getWriteIns(NCompileGraph cg) {
			return cg.getVarWrite(this);
		}
	}

	public static class LocalArgument extends Local {

		public LocalArgument(String name, List<Modifier> modifiers, TypeDef type, NCompileGraph cg, LocalDataGraph parent) {
			super(name, modifiers, type, cg, parent);
		}

		@Override
		public VarLoc getLocation() {
			return VarLoc.STACK_UNDER;
		}
	}
}
