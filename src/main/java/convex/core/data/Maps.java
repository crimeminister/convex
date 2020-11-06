package convex.core.data;

import java.nio.ByteBuffer;
import java.util.HashMap;

import convex.core.exceptions.BadFormatException;

/**
 * Utility class for map functions
 *
 */
public class Maps {

	private static final AMap<?, ?> EMPTY_MAP = MapLeaf.emptyMap();
	private static final Ref<?> EMPTY_REF = EMPTY_MAP.getRef();

	@SuppressWarnings("unchecked")
	public static <K, V, R extends AHashMap<K, V>> R empty() {
		return (R) EMPTY_MAP;
	}

	@SuppressWarnings("unchecked")
	public static <K, V, R extends AMap<K, V>> Ref<R> emptyRef() {
		return (Ref<R>) EMPTY_REF;
	}

	public static <K, V> MapLeaf<K, V> create(K k, V v) {
		return MapLeaf.create(MapEntry.create(k, v));
	}

	/**
	 * Constructs a map with the given keys and values. If keys are repreated, later keys will
	 * overwrite earlier ones.
	 * @param <R>
	 * @param <K>
	 * @param <V>
	 * @param keysAndValues
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <R extends AHashMap<K, V>, K, V> R of(Object... keysAndValues) {
		int n = keysAndValues.length >> 1;
		if (keysAndValues.length != n * 2)
			throw new IllegalArgumentException("Even number of values need for key-value pairs");

		AMap<K, V> result = Maps.empty();
		for (int i = 0; i < n; i++) {
			K key = (K) keysAndValues[i * 2];
			V value = (V) keysAndValues[i * 2 + 1];
			result = result.assoc(key, value);
		}
		return (R) result;
	}

	@SuppressWarnings("unchecked")
	public static <K, V> HashMap<K, V> hashMapOf(Object... keysAndValues) {
		int n = keysAndValues.length >> 1;
		HashMap<K, V> result = new HashMap<>(n);
		if (keysAndValues.length != n * 2)
			throw new IllegalArgumentException("Even number of values need for key-value pairs");
		for (int i = 0; i < n; i++) {
			K key = (K) keysAndValues[i * 2];
			V value = (V) keysAndValues[i * 2 + 1];
			result.put(key, value);
		}
		return result;
	}

	/**
	 * Create a map with a collection of entries.
	 * 
	 * @param <K>
	 * @param <V>
	 * @param entries
	 * @return AHashMap instance
	 */
	public static <K, V> AHashMap<K, V> create(java.util.List<MapEntry<K, V>> entries) {
		return createWithShift(0, entries);
	}

	/**
	 * Create a hashmap with the correct shift and given entries.
	 * 
	 * @param <K>     Key type
	 * @param <V>     Value type
	 * @param shift
	 * @param entries
	 * @return AHashMap instance
	 */
	public static <K, V> AHashMap<K, V> createWithShift(int shift, java.util.List<MapEntry<K, V>> entries) {
		int n = entries.size();
		if (n == 0) return empty();
		AHashMap<K, V> result = Maps.empty();
		for (MapEntry<K, V> e : entries) {
			result = result.assocEntry(e, shift);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static <K, V, R extends AMap<K, V>> R coerce(AMap<?, ?> m) {
		return (R) m;
	}

	/**
	 * Read a Hashmap from a ByteBuffer. Assumes tag byte already read.
	 * @param <K>
	 * @param <V>
	 * @param bb
	 * @return
	 * @throws BadFormatException
	 */
	public static <K, V> AHashMap<K, V> read(ByteBuffer bb) throws BadFormatException {
		long count = Format.readVLCLong(bb);
		if (count <= MapLeaf.MAX_LIST_MAP_SIZE) {
			return MapLeaf.read(bb, count,true);
		} else {
			return MapTree.read(bb, count,true);
		}
	}
	
	/**
	 * Read a Hashmap from a ByteBuffer. Assumes tag byte already read.
	 * @param <K>
	 * @param <V>
	 * @param bb
	 * @return
	 * @throws BadFormatException
	 */
	public static <K, V> AHashMap<K, V> readSet(ByteBuffer bb) throws BadFormatException {
		long count = Format.readVLCLong(bb);
		if (count <= MapLeaf.MAX_LIST_MAP_SIZE) {
			return MapLeaf.read(bb, count,false);
		} else {
			return MapTree.read(bb, count,false);
		}
	}

}
