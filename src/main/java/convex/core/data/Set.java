package convex.core.data;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

import convex.core.crypto.Hash;
import convex.core.data.prim.CVMBool;
import convex.core.exceptions.BadFormatException;
import convex.core.exceptions.InvalidDataException;
import convex.core.lang.RT;
import convex.core.util.Utils;

/**
 * Class implementing a persistent smart set.
 * 
 * Wraps a map, where keys in the map represent the presence of an element in
 * the Set. Map values must be non-null to allow efficient merge operations to
 * distinguish between present and non-present set values.
 * 
 * Encoding:
 * 
 * 0    : Tag.SET
 * 1..n : Equivalent map encoding with true keys (exc. MAP tag)
 *
 * @param <T> The type of set elements
 */
public class Set<T extends ACell> extends ASet<T> {

	static final Set<?> EMPTY = new Set<ACell>(Maps.empty());

	/**
	 * Dummy value used in underlying maps. Not important what this is, but should be small, efficient and non-null
	 * so we use Boolean.TRUE
	 */
	public static final CVMBool DUMMY = CVMBool.TRUE;
	public static final Ref<CVMBool> DUMMY_REF = Ref.TRUE_VALUE;

	/**
	 * Internal map used to represent the set
	 */
	private final AHashMap<T, ACell> map;

	private Set(AHashMap<T, ACell> source) {
		map = source;
	}

	@SuppressWarnings("unchecked")
	static <T extends ACell> Set<T> wrap(AHashMap<T, ACell> source) {
		if (source.isEmpty()) return (Set<T>) EMPTY;
		return new Set<T>(source);
	}

	/**
	 * Create a set using all elements in the given sequence.
	 * 
	 * @param <T> Type of elements
	 * @param a   Any sequence of elements
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	static <T extends ACell> Set<T> create(ASequence<T> a) {
		if (a.isEmpty()) return (Set<T>) EMPTY;

		// dirty, dirty hack because Java doesn't like mutating variables in enclosing
		// scope.
		AHashMap[] m = new AHashMap[] { Maps.empty() };

		// we use the visitor approach to optimise this, because we want to avoid
		// building new Refs for each element.
		a.visitElementRefs(r -> {
			MapEntry<T, ACell> me = MapEntry.createRef(r, Ref.TRUE_VALUE);
			m[0] = m[0].assocEntry(me);
		});
		return Set.wrap(m[0]);
	}

	@SuppressWarnings("unchecked")
	public static <T extends ACell> Set<T> of(Object... elements) {
		AHashMap<T, ACell> m = Maps.empty();
		for (Object o : elements) {
			T e = RT.cvm(o);
			Ref<T> keyRef=(e==null)?(Ref<T>) Ref.NULL_VALUE:e.getRef();
			MapEntry<T, ACell> me = MapEntry.createRef(keyRef, Ref.TRUE_VALUE);
			m = m.assocEntry(me);
		}
		return Set.wrap(m);
	}
	
	public static <T extends ACell> Set<T> create(ACell[] elements) {
		AHashMap<T, ACell> m = Maps.empty();
		for (Object o : elements) {
			T e = RT.cvm(o);
			MapEntry<T, ACell> me = MapEntry.createRef(Ref.get(e), Ref.TRUE_VALUE);
			m = m.assocEntry(me);
		}
		return Set.wrap(m);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return map.containsKey(o);
	}

	@Override
	public Iterator<T> iterator() {
		return map.keySet().iterator();
	}

	@Override
	public Object[] toArray() {
		return map.keySet().toArray();
	}

	@Override
	public <V> V[] toArray(V[] a) {
		return map.keySet().toArray(a);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean containsAll(Collection<?> c) {
		if (c instanceof Set) return ((Set<T>)c).isSubset(this);
		// TODO: maybe faster implementation?
		for (Object o: c) {
			if (!this.contains(o)) return false;
		}
		return true;
	}

	@Override
	public boolean isSubset(Set<T> set) {
		return set.map.containsAllKeys(this.map);
	}

	@Override
	public void ednString(StringBuilder sb) {
		sb.append("#{");
		int size = size();
		for (int i = 0; i < size; i++) {
			if (i > 0) sb.append(',');
			sb.append(Utils.ednString(map.entryAt(i).getKey()));
		}
		sb.append('}');
	}
	
	@Override
	public void print(StringBuilder sb) {
		sb.append("#{");
		int size = size();
		for (int i = 0; i < size; i++) {
			if (i > 0) sb.append(',');
			Utils.print(sb,map.entryAt(i).getKey());
		}
		sb.append('}');
	}

	@Override
	public int getRefCount() {
		return map.getRefCount();
	}

	@Override
	public <R extends ACell> Ref<R> getRef(int i) {
		return map.getRef(i);
	}

	@Override
	public T getByHash(Hash hash) {
		MapEntry<T, ?> me = map.getEntryByHash(hash);
		if (me == null) return null;
		return me.getKey();
	}

	@Override
	public Set<T> updateRefs(IRefFunction func) {
		AHashMap<T, ACell> m = map.updateRefs(func);
		if (map == m) return this;
		return wrap(m);
	}

	@Override
	public boolean isCanonical() {
		return map.isCanonical();
	}

	@Override
	public int encode(byte[] bs, int pos) {
		bs[pos++]=Tag.SET;
		return encodeRaw(bs,pos);
	}

	@Override
	public int encodeRaw(byte[] bs, int pos) {
		return map.write(bs,pos,false);
	}
	
	@Override
	public int estimatedEncodingSize() {
		return 1+map.estimatedEncodingSize();
	}

	/**
	 * Read a set from a ByteBuffer. Assumes tag byte already consumed
	 * @param <T>
	 * @param bb
	 * @return
	 * @throws BadFormatException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ACell> Set<T> read(ByteBuffer bb) throws BadFormatException {
		Object o = Format.read(bb);
		// we need to read the hashmap object directly, and validate it is indeed a
		// hashmap
		if (!(o instanceof AHashMap)) throw new BadFormatException("Map expected as set content");
		AHashMap<T, ACell> map = (AHashMap<T, ACell>) o;

		return wrap(map);
	}

	@Override
	public long count() {
		return map.count();
	}



	@SuppressWarnings("unchecked")
	@Override
	public <R extends ACell> Set<R> include(R a) {
		AHashMap<R, ACell> mymap=(AHashMap<R, ACell>) map;
		if (mymap.containsKey(a)) return (Set<R>) this;
		return wrap(mymap.assocRef(Ref.get(a), DUMMY));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R extends ACell> Set<R> includeRef(Ref<R> ref) {
		AHashMap<R, ACell> mymap=(AHashMap<R, ACell>) map;
		if (mymap.containsKeyRef(ref)) return (Set<R>) this;
		return wrap(mymap.assocRef(ref, DUMMY));
	}
	
	@Override
	public <R extends ACell> Set<R> conj(R a) {
		return include((R) a);
	}

	@Override
	public ASet<T> exclude(T a) {
		return wrap(map.dissoc(a));
	}

	@Override
	public <R extends ACell> Set<R> conjAll(ACollection<R> b) {
		if (b instanceof Set) return includeAll((Set<R>) b);
		ASequence<T> seq = RT.sequence(b);
		if (seq == null) throw new IllegalArgumentException("Can't convert to seq: " + Utils.getClassName(b));
		return conjAll(Set.create(RT.sequence(b)));
	}

	@Override
	public Set<T> disjAll(ACollection<T> b) {
		if (b instanceof Set) return excludeAll((Set<T>) b);
		ASequence<T> seq = RT.sequence(b);
		if (seq == null) throw new IllegalArgumentException("Can't convert to seq: " + Utils.getClassName(b));
		return disjAll(Set.create(seq));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R extends ACell> Set<R> includeAll(Set<R> b) {
		if (b.isEmpty()) return (Set<R>) this;
		
		// any key in either map results in a non-null value, assuming one is non-null
		AHashMap<R, ACell> rmap = ((AHashMap<R, ACell>)map).mergeDifferences(b.map, (x, y) -> (y == null) ? x : y);
		if (map == rmap) return (Set<R>) this;
		return wrap(rmap);
	}

	@Override
	public Set<T> excludeAll(Set<T> b) {
		if (b.isEmpty()) return this;
		
		// any value in y removes the value in x
		AHashMap<T, ACell> rmap = map.mergeWith(b.map, (x, y) -> (y == null) ? x : null);
		if (map == rmap) return this;
		return wrap(rmap);
	}

	@SuppressWarnings("unchecked")
	@Override
	public AVector<T> toVector() {
		return map.getKeys();
	}

	@Override
	public boolean equals(ASet<T> o) {
		if (o == this) return true;
		Set<T> other = (Set<T>) o;
		return map.equalsKeys(other.map);
	}

	@Override
	public void validate() throws InvalidDataException {
		super.validate();
		map.validate();
		map.mapEntries(e -> {
			if (e.getValue() != DUMMY) {
				throw Utils.sneakyThrow(new InvalidDataException(
						"Set must have cureect DUMMY entries in underlying map", this));
			}
			return e;
		});
	}

	@Override
	public void validateCell() throws InvalidDataException {
		map.validateCell();
	}

	@Override
	public <R extends ACell> ASet<R> map(Function<? super T, ? extends R> mapper) {
		return Set.create(this.toVector().map(mapper));
	}

	@Override
	public Set<T> intersectAll(ASet<T> xs) {
		if (!(xs instanceof Set)) throw new UnsupportedOperationException("Must intersect with a set)");
		return intersectAll((Set<T>)xs);
	}

	public Set<T> intersectAll(Set<T> xs) {
		// ensure this is smaller set. Important to avoid creating new set if a subset.
		if (count()>xs.count()) return xs.intersectAll(this);
		
		AHashMap<T, ACell> newMap=map.mergeWith(xs.map, (a,b)->((a==null)||(b==null))?null:a);
		if (map==newMap) return this;
		return wrap(newMap);
	}


}
