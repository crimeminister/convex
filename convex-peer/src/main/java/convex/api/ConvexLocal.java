package convex.api;
 
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import convex.core.Result;
import convex.core.State;
import convex.core.crypto.AKeyPair;
import convex.core.data.ACell;
import convex.core.data.AccountStatus;
import convex.core.data.Address;
import convex.core.data.Cells;
import convex.core.data.Hash;
import convex.core.data.Ref;
import convex.core.data.SignedData;
import convex.core.data.Vectors;
import convex.core.data.prim.CVMLong;
import convex.core.exceptions.MissingDataException;
import convex.core.store.AStore;
import convex.core.transactions.ATransaction;
import convex.core.util.ThreadUtils;
import convex.net.Message;
import convex.net.MessageType;
import convex.peer.Server;

/**
 * Convex Client implementation supporting a direct connection to a Peer Server in the same JVM.
 */
public class ConvexLocal extends Convex {

	private final Server server;

	protected ConvexLocal(Server server, Address address, AKeyPair keyPair) {
		super(address, keyPair);
		this.server=server;
	}
	
	public static ConvexLocal create(Server server, Address address, AKeyPair keyPair) {
		return new ConvexLocal(server, address,keyPair);
	}

	@Override
	public boolean isConnected() {
		return server.isLive();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ACell> CompletableFuture<T> acquire(Hash hash, AStore store) {
		CompletableFuture<T> f = new CompletableFuture<T>();
		ThreadUtils.runVirtual(()-> {
			AStore peerStore=server.getStore();
			Ref<ACell> ref=peerStore.refForHash(hash);
			if (ref==null) {
				f.completeExceptionally(new MissingDataException(peerStore,hash));
			} else {
				ref=store.storeTopRef(ref, Ref.PERSISTED, null);
				f.complete((T) ref.getValue());
			}
		});
		return f;
	}

	@Override
	public CompletableFuture<Result> requestStatus() {
		return makeMessageFuture(MessageType.STATUS,CVMLong.create(makeID()));
	}
	
	@Override
	public CompletableFuture<Result> transact(SignedData<ATransaction> signed) {
		maybeUpdateSequence(signed);
		CompletableFuture<Result> r= makeMessageFuture(MessageType.TRANSACT,Vectors.of(makeID(),signed));
		return r;
	}


	@Override
	public CompletableFuture<Result> requestChallenge(SignedData<ACell> data) {
		return makeMessageFuture(MessageType.CHALLENGE,data);
	}

	@Override
	public CompletableFuture<Result> query(ACell query, Address address) {
		return makeMessageFuture(MessageType.QUERY,Vectors.of(makeID(),query,address));
	}
	
	private long idCounter=0;
	
	private long makeID() {
		return idCounter++;
	}

	private CompletableFuture<Result> makeMessageFuture(MessageType type, ACell payload) {
		CompletableFuture<Result> cf=new CompletableFuture<>();
		Predicate<Message> resultHandler=makeResultHandler(cf);
		Message ml=Message.create(type,payload, resultHandler);
		server.getReceiveAction().accept(ml);
		return cf;
	}

	private Predicate<Message> makeResultHandler(CompletableFuture<Result> cf) {
		return m->{
			Result r=m.toResult();
			if (r.getErrorCode()!=null) {
				sequence=null;
			}
			cf.complete(r);
			return true;
		};
	}

	@Override
	public void close() {
		// Nothing to do
	}

	@Override
	public CompletableFuture<State> acquireState() throws TimeoutException {
		return CompletableFuture.completedFuture(getState());
	}
	
	public State getState() {
		return server.getPeer().getConsensusState();
	}
	
	@Override
	public Server getLocalServer() {
		return server;
	}
	
	@Override
	public long getSequence() {
		if (sequence==null) {
			AccountStatus as=getState().getAccount(address);
			if (as==null) return 0;
			sequence=as.getSequence();
		}
		return sequence;
	}
	
	@Override
	public long getSequence(Address addr) {
		if (Cells.equals(address, addr)) return getSequence();
		return getState().getAccount(addr).getSequence();
	}

	@Override
	public String toString() {
		return "Local Convex instance on "+server.getHostAddress();
	}

	@Override
	public InetSocketAddress getHostAddress() {
		return server.getHostAddress();
	}

	@Override
	public Long getBalance() {
		return server.getPeer().getConsensusState().getBalance(address);
	}

}
