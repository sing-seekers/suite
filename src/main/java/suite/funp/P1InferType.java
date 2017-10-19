package suite.funp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Fixie_.FixieFun1;
import suite.adt.pair.Fixie_.FixieFun2;
import suite.adt.pair.Pair;
import suite.fp.Unify;
import suite.fp.Unify.UnNode;
import suite.funp.Funp_.Funp;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpArray;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpFixed;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpIndex;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpPolyType;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpTree2;
import suite.funp.P0.FunpVariable;
import suite.funp.P1.FunpAllocStack;
import suite.funp.P1.FunpData;
import suite.funp.P1.FunpInvokeInt;
import suite.funp.P1.FunpInvokeInt2;
import suite.funp.P1.FunpInvokeIo;
import suite.funp.P1.FunpMemory;
import suite.funp.P1.FunpRoutine;
import suite.funp.P1.FunpRoutine2;
import suite.funp.P1.FunpRoutineIo;
import suite.funp.P1.FunpSaveRegisters;
import suite.immutable.IMap;
import suite.inspect.Inspect;
import suite.node.io.TermOp;
import suite.node.util.Singleton;
import suite.primitive.IntPrimitives.Obj_Int;
import suite.primitive.adt.pair.IntIntPair;
import suite.streamlet.Read;
import suite.util.AutoObject;
import suite.util.Rethrow;
import suite.util.String_;
import suite.util.Switch;

/**
 * Hindley-Milner type inference.
 *
 * @author ywsing
 */
public class P1InferType {

	private Inspect inspect = Singleton.me.inspect;

	private int is = Funp_.integerSize;
	private int ps = Funp_.pointerSize;

	private UnNode<Type> typeBoolean = new TypeBoolean();
	private UnNode<Type> typeNumber = new TypeNumber();
	private Map<Funp, UnNode<Type>> typeByNode = new HashMap<>();

	public Funp infer(Funp n) {
		return infer(n, unify.newRef());
	}

	public Funp infer(Funp n0, UnNode<Type> t) {
		UnNode<Type> t0 = typeNumber;
		UnNode<Type> t1 = TypeLambda.of(typeNumber, t0);
		UnNode<Type> t2 = TypeLambda.of(typeNumber, t1);
		IMap<String, UnNode<Type>> env = IMap.<String, UnNode<Type>> empty() //
				.put(TermOp.BIGAND.name, t2) //
				.put(TermOp.BIGOR_.name, t2) //
				.put(TermOp.PLUS__.name, t2) //
				.put(TermOp.MINUS_.name, t2) //
				.put(TermOp.MULT__.name, t2) //
				.put(TermOp.DIVIDE.name, t2);

		if (unify.unify(t, new Infer(env).infer(n0)))
			return erase(0, 0, IMap.empty(), n0);
		else
			throw new RuntimeException("cannot infer type for " + n0);
	}

	private class Infer {
		private IMap<String, UnNode<Type>> env;

		private Infer(IMap<String, UnNode<Type>> env) {
			this.env = env;
		}

		private UnNode<Type> infer(Funp n0) {
			UnNode<Type> t = typeByNode.get(n0);
			if (t == null)
				typeByNode.put(n0, t = infer_(n0));
			return t;
		}

		private UnNode<Type> infer_(Funp n0) {
			Switch<UnNode<Type>> sw = new Switch<>(n0);

			sw.applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
				TypeLambda tl = (TypeLambda) infer(lambda);
				unify(n0, tl.parameterType, infer(value));
				return tl.returnType;
			})).applyIf(FunpArray.class, f -> f.apply(elements -> {
				UnNode<Type> te = unify.newRef();
				for (Funp element : elements)
					unify(n0, te, infer_(element));
				return TypeArray.of(te, elements.size());
			})).applyIf(FunpBoolean.class, f -> {
				return typeBoolean;
			}).applyIf(FunpDeref.class, f -> f.apply(pointer -> {
				UnNode<Type> t = unify.newRef();
				unify(n0, TypeReference.of(t), infer(pointer));
				return t;
			})).applyIf(FunpDefine.class, f -> f.apply((var, value, expr) -> {
				return new Infer(env.put(var, infer(value))).infer(expr);
			})).applyIf(FunpField.class, f -> f.apply((struct, field) -> {
				return Read //
						.from(((TypeStruct) infer(struct)).pairs) //
						.filter(pair -> String_.equals(pair.t0, field)) //
						.uniqueResult().t1;
			})).applyIf(FunpFixed.class, f -> f.apply((var, expr) -> {
				UnNode<Type> t = unify.newRef();
				unify(n0, t, new Infer(env.put(var, t)).infer(expr));
				return t;
			})).applyIf(FunpIf.class, f -> f.apply((if_, then, else_) -> {
				UnNode<Type> t;
				unify(n0, typeBoolean, infer(if_));
				unify(n0, t = infer(then), infer(else_));
				return t;
			})).applyIf(FunpIndex.class, f -> f.apply((array, index) -> {
				UnNode<Type> t = unify.newRef();
				unify(n0, TypeArray.of(t), infer(array));
				return t;
			})).applyIf(FunpLambda.class, f -> f.apply((var, expr) -> {
				UnNode<Type> tv = unify.newRef();
				return TypeLambda.of(tv, new Infer(env.put(var, tv)).infer(expr));
			})).applyIf(FunpNumber.class, f -> {
				return typeNumber;
			}).applyIf(FunpPolyType.class, f -> f.apply(expr -> {
				return unify.clone(infer(expr));
			})).applyIf(FunpReference.class, f -> f.apply(expr -> {
				return TypeReference.of(infer(expr));
			})).applyIf(FunpStruct.class, f -> f.apply(pairs -> {
				return TypeStruct.of(Read.from2(pairs).mapValue(this::infer).toList());
			})).applyIf(FunpTree.class, f -> f.apply((operator, left, right) -> {
				unify(n0, infer(left), typeNumber);
				unify(n0, infer(right), typeNumber);
				return typeNumber;
			})).applyIf(FunpTree2.class, f -> f.apply((operator, left, right) -> {
				unify(n0, infer(left), typeNumber);
				unify(n0, infer(right), typeNumber);
				return typeNumber;
			})).applyIf(FunpVariable.class, f -> f.apply(var -> {
				return env.get(var);
			}));

			return sw.nonNullResult();
		}
	}

	private void unify(Funp n0, UnNode<Type> type0, UnNode<Type> type1) {
		if (!unify.unify(type0, type1))
			throw new RuntimeException("cannot infer type for " + n0);
	}

	private Funp erase(int scope, int fs, IMap<String, Var> env, Funp n) {
		return new Erase(scope, fs, env).erase(n);
	}

	private class Erase {
		private int scope;
		private int fs;
		private IMap<String, Var> env;

		private Erase(int scope, int fs, IMap<String, Var> env) {
			this.scope = scope;
			this.fs = fs;
			this.env = env;
		}

		private Funp erase(Funp n) {
			return inspect.rewrite(Funp.class, this::erase_, n);
		}

		private Funp erase_(Funp n0) {
			Switch<Funp> sw = new Switch<>(n0);

			sw.applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
				if (Boolean.TRUE || !(lambda instanceof FunpLambda)) {
					LambdaType lt = lambdaType(lambda);
					Funp lambda1 = erase(lambda);
					Funp invoke;
					if (lt.os == ps)
						invoke = allocStack(value, FunpInvokeInt.of(lambda1));
					else if (lt.os == ps * 2)
						invoke = allocStack(value, FunpInvokeInt2.of(lambda1));
					else
						invoke = FunpAllocStack.of(lt.os, null, allocStack(value, FunpInvokeIo.of(lambda1)));
					return FunpSaveRegisters.of(invoke);
				} else {
					FunpLambda lambda1 = (FunpLambda) lambda;
					return erase(FunpDefine.of(lambda1.var, value, lambda1.expr));
				}
			})).applyIf(FunpArray.class, f -> f.apply(elements -> {
				UnNode<Type> elementType = ((TypeArray) typeOf(n0)).elementType.final_();
				int elementSize = getTypeSize(elementType);
				int offset = 0;
				List<Pair<Funp, IntIntPair>> list = new ArrayList<>();
				for (Funp element : elements) {
					int offset0 = offset;
					list.add(Pair.of(element, IntIntPair.of(offset0, offset += elementSize)));
				}
				return FunpData.of(list);
			})).applyIf(FunpDefine.class, f -> f.apply((var, value, expr) -> {
				if (Boolean.TRUE) {
					int fs1 = fs - getTypeSize(typeOf(value));
					Erase erase1 = new Erase(scope, fs1, env.put(var, new Var(scope, fs1, fs)));
					return allocStack(value, erase1.erase(expr));
				} else
					return erase(new Expand(var, value).expand(expr));
			})).applyIf(FunpDeref.class, f -> f.apply(pointer -> {
				return FunpMemory.of(erase(pointer), 0, getTypeSize(typeOf(n0)));
			})).applyIf(FunpField.class, f -> f.apply((struct, field) -> {
				List<Pair<String, UnNode<Type>>> pairs = ((TypeStruct) typeOf(struct)).pairs;
				int offset = 0;
				for (Pair<String, UnNode<Type>> pair : pairs)
					if (!String_.equals(pair.t0, field))
						offset += getTypeSize(pair.t1);
					else
						return FunpMemory.of(getAddress(erase(struct)), offset, offset + getTypeSize(pair.t1));
				throw new RuntimeException();
			})).applyIf(FunpIndex.class, f -> f.apply((array, index) -> {
				int size = getTypeSize(((TypeArray) typeOf(array)).elementType);
				Funp address0 = getAddress(erase(array));
				FunpTree inc = FunpTree.of(TermOp.MULT__, erase(index), FunpNumber.of(size));
				Funp address1 = FunpTree.of(TermOp.PLUS__, address0, inc);
				return FunpMemory.of(address1, 0, size);
			})).applyIf(FunpLambda.class, f -> f.apply((var, expr) -> {
				int b = ps * 2; // return address and EBP
				int scope1 = scope + 1;
				LambdaType lt = lambdaType(n0);
				Erase erase1 = new Erase(scope1, 0, env.put(var, new Var(scope1, b, b + lt.is)));
				Funp expr1 = erase1.erase(expr);
				if (lt.os == ps)
					return FunpRoutine.of(expr1);
				else if (lt.os == ps * 2)
					return FunpRoutine2.of(expr1);
				else
					return FunpRoutineIo.of(expr1, lt.is, lt.os);
			})).applyIf(FunpPolyType.class, f -> f.apply(expr -> {
				return erase(expr);
			})).applyIf(FunpReference.class, f -> f.apply(expr -> {
				return getAddress(erase(expr));
			})).applyIf(FunpStruct.class, f -> f.apply(fvs -> {
				Iterator<Pair<String, UnNode<Type>>> ftsIter = ((TypeStruct) typeOf(n0)).pairs.iterator();
				int offset = 0;
				List<Pair<Funp, IntIntPair>> list = new ArrayList<>();
				for (Pair<String, Funp> fv : fvs) {
					int offset0 = offset;
					list.add(Pair.of(fv.t1, IntIntPair.of(offset0, offset += getTypeSize(ftsIter.next().t1))));
				}
				return FunpData.of(list);
			})).applyIf(FunpVariable.class, f -> f.apply(var -> {
				return getVariable(env.get(var));
			}));

			return sw.result();
		}

		private Funp allocStack(Funp p, Funp expr) {
			UnNode<Type> t = typeOf(p);
			return FunpAllocStack.of(getTypeSize(t), erase(p), expr);
		}

		private Funp getAddress(Funp n0) {
			Switch<Funp> sw = new Switch<>(n0);

			sw.applyIf(FunpMemory.class, f -> f.apply((pointer, start, end) -> {
				return FunpTree.of(TermOp.PLUS__, pointer, FunpNumber.of(start));
			})).applyIf(FunpVariable.class, f -> f.apply(var -> {
				return getAddress(getVariable(env.get(var)));
			}));

			return sw.nonNullResult();
		}

		private Funp getVariable(Var vd) {
			Funp nfp = Funp_.framePointer;
			for (int i = scope; i < vd.scope; i++)
				nfp = FunpMemory.of(nfp, 0, ps);
			return FunpMemory.of(nfp, vd.start, vd.end);
		}
	}

	private class Expand {
		private String var;
		private Funp value;

		private Expand(String var, Funp value) {
			this.var = var;
			this.value = value;
		}

		private Funp expand(Funp n) {
			return inspect.rewrite(Funp.class, this::expand_, n);
		}

		private Funp expand_(Funp n0) {
			Switch<Funp> sw = new Switch<>(n0);

			// variable re-defined
			sw.applyIf(FunpDefine.class, f -> f.apply((var_, value, expr) -> {
				return String_.equals(var_, var) ? n0 : null;
			})).applyIf(FunpVariable.class, f -> f.apply(var_ -> {
				return String_.equals(var_, var) ? value : n0;
			}));

			return sw.result();
		}
	}

	private class Var {
		private int scope;
		private int start;
		private int end;

		public Var(int scope, int start, int end) {
			this.scope = scope;
			this.start = start;
			this.end = end;
		}
	}

	private LambdaType lambdaType(Funp lambda) {
		LambdaType lt = new LambdaType(lambda);
		if (lt.os <= is)
			return lt;
		else
			throw new RuntimeException();

	}

	private class LambdaType {
		private int is, os;

		private LambdaType(Funp lambda) {
			TypeLambda lambdaType = (TypeLambda) typeOf(lambda);
			is = getTypeSize(lambdaType.parameterType);
			os = getTypeSize(lambdaType.returnType);
		}
	}

	private UnNode<Type> typeOf(Funp n) {
		return typeByNode.get(n);
	}

	private int getTypeSize(UnNode<Type> n) {
		Switch<Integer> sw = new Switch<>(n.final_());
		sw.applyIf(TypeArray.class, t -> t.apply((elementType, size) -> {
			return getTypeSize(elementType) * size;
		})).applyIf(TypeLambda.class, t -> {
			return ps + ps;
		}).applyIf(TypeNumber.class, t -> {
			return is;
		}).applyIf(TypeReference.class, t -> {
			return ps;
		}).applyIf(TypeStruct.class, t -> t.apply(pairs -> {
			return Read.from(pairs).collectAsInt(Obj_Int.sum(field -> getTypeSize(field.t1)));
		}));
		return sw.result().intValue();
	}

	private static Unify<Type> unify = new Unify<>();

	private static class TypeArray extends Type {
		private UnNode<Type> elementType;
		private int size;

		private static TypeArray of(UnNode<Type> elementType) {
			return TypeArray.of(elementType, -1);
		}

		private static TypeArray of(UnNode<Type> elementType, int size) {
			TypeArray t = new TypeArray();
			t.elementType = elementType;
			t.size = size;
			return t;
		}

		private <R> R apply(FixieFun2<UnNode<Type>, Integer, R> fun) {
			return fun.apply(elementType, size);
		}

		public boolean unify(UnNode<Type> type) {
			if (getClass() == type.getClass()) {
				TypeArray other = (TypeArray) type;
				if (unify.unify(elementType, other.elementType)) {
					if (size == -1)
						size = other.size;
					else if (other.size == -1)
						other.size = size;
					return size == other.size;
				} else
					return false;
			} else
				return false;
		}
	}

	private static class TypeBoolean extends Type {
	}

	private static class TypeLambda extends Type {
		private UnNode<Type> parameterType, returnType;

		private static TypeLambda of(UnNode<Type> parameterType, UnNode<Type> returnType) {
			TypeLambda t = new TypeLambda();
			t.parameterType = parameterType;
			t.returnType = returnType;
			return t;
		}
	}

	private static class TypeNumber extends Type {
	}

	private static class TypeStruct extends Type {
		private List<Pair<String, UnNode<Type>>> pairs;

		private static TypeStruct of(List<Pair<String, UnNode<Type>>> pairss) {
			TypeStruct t = new TypeStruct();
			t.pairs = pairss;
			return t;
		}

		private <R> R apply(FixieFun1<List<Pair<String, UnNode<Type>>>, R> fun) {
			return fun.apply(pairs);
		}

		public boolean unify(UnNode<Type> type) {
			if (getClass() == type.getClass()) {
				TypeStruct other = (TypeStruct) type;
				List<Pair<String, UnNode<Type>>> pairs0 = pairs;
				List<Pair<String, UnNode<Type>>> pairs1 = other.pairs;
				int size = pairs0.size();
				if (size == pairs1.size()) {
					boolean b = true;
					for (int i = 0; i < size; i++) {
						Pair<String, UnNode<Type>> pair0 = pairs0.get(i);
						Pair<String, UnNode<Type>> pair1 = pairs1.get(i);
						b &= String_.equals(pair0.t0, pair1.t0) && unify.unify(pair0.t1, pair1.t1);
					}
					return b;
				} else
					return false;
			} else
				return false;
		}
	}

	private static class TypeReference extends Type {
		@SuppressWarnings("unused")
		private UnNode<Type> type;

		private static TypeReference of(UnNode<Type> type) {
			TypeReference t = new TypeReference();
			t.type = type;
			return t;
		}
	}

	private static class Type extends AutoObject<Type> implements UnNode<Type> {
		public boolean unify(UnNode<Type> type) {
			return getClass() == type.getClass() //
					&& fields().isAll(field -> Rethrow.ex(() -> unify.unify(cast(field.get(this)), cast(field.get(type)))));
		}

		private static UnNode<Type> cast(Object object) {
			@SuppressWarnings("unchecked")
			UnNode<Type> node = (UnNode<Type>) object;
			return node;
		}
	}

}
