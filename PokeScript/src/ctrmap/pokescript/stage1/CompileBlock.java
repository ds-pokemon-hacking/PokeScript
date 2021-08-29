package ctrmap.pokescript.stage1;

import ctrmap.pokescript.data.Variable;
import ctrmap.pokescript.instructions.ctr.instructions.PConditionJump;
import ctrmap.pokescript.instructions.abstractcommands.AInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainInstruction;
import ctrmap.pokescript.instructions.abstractcommands.APlainOpCode;
import ctrmap.pokescript.stage0.Statement;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class CompileBlock {

	public NCompileGraph graph;
	
	public String fullBlockName;
	private String blockName;
	public List<Variable> localsOfThisBlock = new ArrayList<>();
	public List<BlockAttribute> attributes = new ArrayList<>();

	public CompileBlock chainPredecessor;

	public List<AInstruction> blockEndInstructions = new ArrayList<>();
	public AInstruction blockEndControlInstruction;
	public boolean explicitAdjustStack = false;
	public boolean popNext = false;
	public APlainInstruction blockBeginAdjustStackIns;

	public static final SecureRandom RNG = new SecureRandom();
	
	public CompileBlock(NCompileGraph cg) {
		blockName = "NullBlock_" + RNG.nextInt();
		blockBeginAdjustStackIns = cg.getPlain(APlainOpCode.RESIZE_STACK, 0);
		this.graph = cg;
	}

	public CompileBlock(Statement statement, NCompileGraph cg) {
		this(cg);
		blockName = statement + "_" + RNG.nextInt();
		switch (statement) {
			case IF:
			case ELSE_IF:
				attributes.add(BlockAttribute.NEGATEABLE);
				break;
		}
		switch (statement) {
			case FOR:
			case SWITCH:
			case WHILE:
				attributes.add(BlockAttribute.BREAKABLE);
				break;
		}
		switch (statement) {
			case FOR:
			case WHILE:
				attributes.add(BlockAttribute.LOOP);
				break;
		}
	}

	public void setChainJumpArg0(String chainTarget) {
		if (blockEndControlInstruction != null) {
			((PConditionJump) blockEndControlInstruction).targetLabel = chainTarget;
		}
		if (chainPredecessor != null) {
			chainPredecessor.setChainJumpArg0(chainTarget);
		}
	}

	public String getShortBlockName() {
		return blockName;
	}

	public void setShortBlockLabel(String str) {
		blockName = str;
	}

	public String getBlockTermLabel() {
		return "term";
	}

	public String getBlockHaltLabel() {
		return "halt";
	}

	public String getBlockTermFullLabel() {
		return fullBlockName + "_" + getBlockTermLabel();
	}

	public String getBlockHaltFullLabel() {
		return fullBlockName + "_" + getBlockHaltLabel();
	}

	public boolean isBreakable() {
		return hasAttribute(BlockAttribute.BREAKABLE);
	}

	public boolean isLoop() {
		return hasAttribute(BlockAttribute.LOOP);
	}

	public boolean hasAttribute(BlockAttribute a) {
		return attributes.contains(a);
	}

	public void gracefullyTerminate(NCompileGraph cg) {
		//un-allocate the variables
		updateBlockStackIns();
		cg.removeLocals(localsOfThisBlock);
	}
	
	public void updateBlockStackIns(){
		blockBeginAdjustStackIns.args[0] = localsOfThisBlock.size() * graph.provider.getMemoryInfo().getStackIndexingStep();
	}

	public static enum BlockAttribute {
		BREAKABLE,
		LOOP,
		NEGATEABLE
	}
}
