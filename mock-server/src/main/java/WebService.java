import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import load.DotLoader;
import load.Loader;
import load.LoaderException;
import model.InOutRule;
import model.LTS;
import model.OutInRule;
import model.RequestT;
import model.ResponseT;
import model.Rule;
import model.State;
import model.Transition;

import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@WebServlet(urlPatterns = {"/*"})
public class WebService extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private List<Rule> rules = new ArrayList<>();
	private LTS dot;
	private State pos;
	private Javalin app;
	private Map<String, InOutHandler> handlers = new HashMap<>();
	private long lastAction;
	private ArrayList<Long> fifo = new ArrayList<Long>();

	/**
	 * This method starts the web server. The server is listening on 3 paths:
	 * /config, /rules and /attack.
	 * It also collects the content type of the HTTP request.
	 */
	public WebService() {
		app = Javalin.createStandalone(config -> {
			config.requestLogger((ctx, executionTimeMs) -> LoggerFactory.getLogger("MOCK").info(String.format("%s on %s -> %d", ctx.method(), ctx.fullUrl(), ctx.res.getStatus())));
		});
		app.exception(LoaderException.class, ExceptionHandlers.genericHandler(400));
		app.exception(RuleAlreadyExistsException.class, ExceptionHandlers.genericHandler(400));
		app.exception(Exception.class, (exception, ctx) -> {
			exception.printStackTrace();
			ctx.status(500);
			ctx.result(String.format("%s, %s", exception.getClass().toString(), exception.getMessage()));
		});

		app.post("/rules", DotHandler());
		//app.post("/attack",attackHandler()); TODO
		app.get("/", ctx -> ctx.result("Mock: It works !"));
	}

	/**
	 * This method is called when the user does a HTTP POST request to the
	 * attack path of the mock IP address. It calls the correct attack methods
	 * according to the user's choice.
	 */
	/*private Handler attackHandler() {
		return ctx -> {
			if(getRules().size() != 0) {
				Attacker attacker = new Attacker(getRules());
				ctx.status(204);
				if(Objects.equals(ctx.queryParam("type"), "all")) {
					attacker.XSSAttacks();
					attacker.httpFloodAttack();
					attacker.robustnessAttacks();
				} else if (Objects.equals(ctx.queryParam("type"), "httpflood")) {
					attacker.httpFloodAttack();
				} else if(Objects.equals(ctx.queryParam("type"), "xss")) {
					attacker.XSSAttacks();
				} else if(Objects.equals(ctx.queryParam("type"),"robustness")) {
					attacker.robustnessAttacks();
				}
				else {
					ctx.result("Error: wrong/no attack type given;");
					ctx.status(400);
				}
				attacker.attack();
			} else {
				ctx.result("Error: no rules found.");
				ctx.status(400);
			}
		};
	}*/

	private void buildInputRequestHandler() {
		//TODO make for the post to
		for (String path: dot.getInputRequests()) {
			app.get(path, ctx -> {
				long time = System.currentTimeMillis();
				LoggerFactory.getLogger("MOCK").info(String.format("received : " + time));
				fifo.add(time);
				synchronized(this){
					RequestT t = dot.getReq(ctx.fullUrl(), pos); // min weight ?
					long now = System.currentTimeMillis();
					if (t !=  null) {
						if (t.getDelay() == 0 || lastAction != 0 || t.getDelay() < now - lastAction) {
							LoggerFactory.getLogger("MOCK").info(String.format("in : " + Long.toString(time)));
							while (pos.isInit() && getMin(fifo) != time) {
								this.wait();
							}							
							pos = t.getTarget();
							lastAction = now;
							while (runMock(true) == true);
							ResponseT resp = t.getResponse();
							lastAction = System.currentTimeMillis();
							ctx.result(resp.getBody());
							ctx.status(resp.getStatus());
							Thread.sleep(50);////
							if (resp.getContent() != null) {
								ctx.contentType(resp.getContent());
							}
							for (String h: resp.getHeaders().keySet()) {
								ctx.header(h, resp.getHeaders().get(h));
							}

							fifo.remove(time);
							LoggerFactory.getLogger("MOCK").info(String.format("fifo :" + fifo));
							LoggerFactory.getLogger("MOCK").info(String.format("outini : " + time + " req : " + ctx.fullUrl()));
							if (pos.equals(resp.getSource())) {
								pos = resp.getTarget();
								if (pos.isFinal()) {
									pos = dot.getInitialState();
								}
							}
							else {
								ctx.status(502);
								System.err.println("error: response " + resp.toString() + "launched from state " + pos.toString());
							}
						}
						else {
							fifo.remove(time);
							ctx.result("request received too late.");
							ctx.status(501);
							System.err.println("request received too late.");
						}
					}
					else {
						RequestT t2 = dot.getReq(ctx.fullUrl(), dot.getInitialState()); // min weight ?
						if (t2 !=  null) {
							this.wait();
							while (getMin(fifo) != time) {
								this.wait();
							}							
							pos = t2.getTarget();
							lastAction = System.currentTimeMillis();
							while (runMock(true) == true);
							ResponseT resp = t2.getResponse();
							lastAction = System.currentTimeMillis();
							ctx.result(resp.getBody());
							ctx.status(resp.getStatus());
							Thread.sleep(10);////
							if (resp.getContent() != null) {
								ctx.contentType(resp.getContent());
							}
							for (String h: resp.getHeaders().keySet()) {
								ctx.header(h, resp.getHeaders().get(h));
							}
							fifo.remove(time);
							if (pos.equals(resp.getSource())) {
								pos = resp.getTarget();
								if (pos.isFinal()) {
									pos = dot.getInitialState();
								}
							}
							else {
								ctx.status(502);
								System.err.println("error: response " + resp.toString() + "launched from state " + pos.toString());
							}
							LoggerFactory.getLogger("MOCK").info(String.format("out : " +  time	 + " req : " + ctx.fullUrl()));
						}
						else {
							fifo.remove(time);
							ctx.result("request received at the wrong position in the model.");
							ctx.status(400);
							System.err.println("not now, i'm busy.");
						}
					}
				}
			});
		}
	}

	static long getMin(ArrayList<Long> a) {
		long res = a.get(0);
		for (long l : a) {
			if (res > l) {
				res = l;
			}
		}
		return res;
	}

	/**
	 * This method is called when the user does a HTTP POST request to the rules
	 * path of the mock IP address. It loads the rules given with the JSON or YAML loader.
	 */
	private Handler DotHandler() {
		return ctx -> {
			Loader loader;
			if (Objects.equals(ctx.header("Content-Type"), "text/plain")) {
				loader = new DotLoader();
			}
			else {
				throw new LoaderException("Wrong content type, \"text/plain\" was expected");
			}
			dot = loader.load(ctx.body());
			pos = dot.getInitialState();
			buildInputRequestHandler();
			lastAction = System.currentTimeMillis();
			ctx.status(204);
			//for (int i = 0 ; i < 10; i++) { //nb session run by the mock
			while(true) {
				if (pos.getMaxDelay() > System.currentTimeMillis() - lastAction || pos.getMaxDelay() == 0) {//TODO 
					Thread.sleep(10);
					continue;
				}
				synchronized(this){
					while (runMock(false) == true);
					if (pos.isInit()) {
						this.notifyAll();
					}
					if (pos.getMaxDelay() > System.currentTimeMillis() - lastAction || pos.getMaxDelay() == 0) {// sort du synchronized pour reception de la requete en attente.
						continue;
					}
					pos = dot.getInitialState();
					this.notifyAll();
				}
			}
		};
	}

	/** TODO use the weight, the delay, and the repetition 
	 * @throws InterruptedException **/
	private boolean runMock(boolean passOutResp) throws InterruptedException {
		if (!pos.isInit() && pos.getInResp() != null) {
			long now = System.currentTimeMillis();
			pos = pos.getInResp().getTarget();
			lastAction = now;
			return true;
		}
		if (pos.getMaxDelay() > System.currentTimeMillis() - lastAction || pos.getMaxDelay() == 0) {
			Thread.sleep(10);
			return false; // sortie du run et du synchronised pour laisser le thread attendre la requete en entrée.
		}
		if (!passOutResp && (pos.plannedResponse() > System.currentTimeMillis() - lastAction || pos.plannedResponse() == 0)) {
			Thread.sleep(10);
			return true;
		}
		RequestT output = pos.getOutReq(lastAction);
		if (output != null) {
			output.incWeight();
			OutputRequest send = new OutputRequest(output);
			lastAction = System.currentTimeMillis();;
			send.run();
			pos = output.getTarget();
			if (!send.getMatch()) {
				//TODO error
			}
			return true;
		}
		if (!pos.getFutureOutReq().isEmpty()) {
			Thread.sleep(10);
			return true;
		}
		return false;
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		app.servlet().service(req, resp);
	}
}
