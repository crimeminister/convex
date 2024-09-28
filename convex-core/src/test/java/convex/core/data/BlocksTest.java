package convex.core.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import convex.core.Constants;
import convex.core.cpos.Block;
import convex.core.crypto.AKeyPair;
import convex.core.exceptions.BadFormatException;
import convex.core.exceptions.BadSignatureException;
import convex.core.init.InitTest;
import convex.core.transactions.ATransaction;
import convex.core.transactions.Transfer;

public class BlocksTest {

	@Test
	public void testEquality() throws BadFormatException {
		long ts = System.currentTimeMillis();
		Block b1 = Block.create(ts, Vectors.empty());
		Block b2 = Block.create(ts, Vectors.empty());

		ObjectsTest.doEqualityTests(b1,b2);
		
		assertEquals(0,b1.getTransactions().count());

		RecordTest.doRecordTests(b1);
	}
	
	@Test public void testValues() {
		long ts = System.currentTimeMillis();
		Block b1 = Block.create(ts, Vectors.empty());
		
		assertEquals(2,b1.values().count());
	}
	
	@Test
	public void testTransactions() throws BadSignatureException {
		AKeyPair kp = InitTest.HERO_KEYPAIR;

		ATransaction t = Transfer.create(InitTest.HERO,0, InitTest.VILLAIN, 1000);
		SignedData<ATransaction> st = kp.signData(t);

		long ts = System.currentTimeMillis();
		Block b = Block.create(ts, Vectors.of(st));
		assertEquals(1, b.length());
		assertEquals(t, b.getTransactions().get(0).getValue());

		RecordTest.doRecordTests(b);

	}
	
	@Test 
	public void testBlockRefs() {
		long ts=Constants.INITIAL_TIMESTAMP;
		ATransaction t = Transfer.create(InitTest.HERO,0, InitTest.VILLAIN, 1000);
		Block b1 = Block.create(ts, Vectors.of(t,t,t));

		assertEquals(3,b1.getRefCount());

	}
}
