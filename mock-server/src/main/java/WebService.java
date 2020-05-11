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

@WebServlet(urlPatterns = {"/*"})
public class WebService extends HttpServlet {
	private List<Rule> rules = new ArrayList<>();
	private LTS dot;
	private State pos;
	private Javalin app;
	private Map<String, InOutHandler> handlers = new HashMap<>();
	private long lastAction;

	/**
	 * This method starts the web server. The server is listening on 3 paths:
	 * /config, /rules and /attack.
	 * It also collects the content type of the HTTP request.
	 */
	public WebService() {
		app = Javalin.createStandalone(config -> {
			config.requestLogger((ctx, executionTimeMs) -> LoggerFactory.getLogger("MOCK").info(String.format("%s on %s -> %d", ctx.method(), ctx.url(), ctx.res.getStatus())));
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
		for (String path: dot.getInputRequests()) {
			app.get(path, ctx -> {
				synchronized(this){
					RequestT t = dot.getReq(ctx.fullUrl(), pos); // min weight ?
					long now = System.currentTimeMillis();//TODO make for the post to
					LoggerFactory.getLogger("MOCK").info(String.format(pos.toString()));
					if (t !=  null) {
						if (t.getDelay() == 0 || lastAction != 0 || t.getDelay() < now - lastAction) {
							pos = t.getTarget();
							//LoggerFactory.getLogger("MOCK").info(String.format(t.getTarget().toString()));
							lastAction = now;
							while (runMock(true) == true);
							ResponseT resp = t.getResponse();
							lastAction = System.currentTimeMillis();
							ctx.result(resp.getBody());
							ctx.status(resp.getStatus());
							if (resp.getContent() != null) {
								ctx.contentType(resp.getContent());
							}
							for (String h: resp.getHeaders().keySet()) {
								ctx.header(h, resp.getHeaders().get(h));
							}
							if (pos.equals(resp.getSource())) {
								//LoggerFactory.getLogger("MOCK").info(String.format("end: " + resp.getTarget().toString()));
								pos = resp.getTarget();
								if (pos.isFinal()) {
									//LoggerFactory.getLogger("MOCK").info(String.format("init"));
									pos = dot.getInitialState();
									this.notify();
								}
							}
							else {
								ctx.status(502);
								System.err.println("error: response " + resp.toString() + "launched from state " + pos.toString());
							}

						}
						else {
							ctx.result("request received too late.");
							ctx.status(501);
							System.err.println("request received too late.");
						}
					}
					else {
						RequestT t2 = dot.getReq(ctx.fullUrl(), dot.getInitialState()); // min weight ?
						if (t2 !=  null) {
							this.wait();
							pos = t2.getTarget();
							//LoggerFactory.getLogger("MOCK").info(String.format(t.getTarget().toString()));
							lastAction = System.currentTimeMillis();
							while (runMock(true) == true);
							ResponseT resp = t2.getResponse();
							lastAction = System.currentTimeMillis();
							ctx.result(resp.getBody());
							ctx.status(resp.getStatus());
							if (resp.getContent() != null) {
								ctx.contentType(resp.getContent());
							}
							for (String h: resp.getHeaders().keySet()) {
								ctx.header(h, resp.getHeaders().get(h));
							}
							if (pos.equals(resp.getSource())) {
								//LoggerFactory.getLogger("MOCK").info(String.format("end: " + resp.getTarget().toString()));
								pos = resp.getTarget();
								if (pos.isFinal()) {
									//LoggerFactory.getLogger("MOCK").info(String.format("init"));
									pos = dot.getInitialState();	
									this.notify();
								}
							}
							else {
								ctx.status(502);
								System.err.println("error: response " + resp.toString() + "launched from state " + pos.toString());
							}
	
						}
						else {
							ctx.result("request received at the wrong position in the model.");
							ctx.status(400);
							System.err.println("not now, i'm busy.");
						}
					}
				}
			});
		}
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
				//LoggerFactory.getLogger("MOCK").info(String.format(pos.toString()));
				if (pos.getMaxDelay() > System.currentTimeMillis() - lastAction || pos.getMaxDelay() == 0) {//TODO 
					//LoggerFactory.getLogger("MOCK").info(String.format("wait"));
					Thread.sleep(50);
					continue;
				}
				//LoggerFactory.getLogger("MOCK").info(String.format("stop waiting"));
				while (runMock(false) == true);
				LoggerFactory.getLogger("MOCK").info(String.format("init"));
				synchronized(this){
					pos = dot.getInitialState();
					this.notify();
				}
			}
		};
	}

	/** TODO use the weight, the delay, and the repetition 
	 * @throws InterruptedException **/
	private boolean runMock(boolean passOutResp) throws InterruptedException {
		LoggerFactory.getLogger("MOCK").info(String.format("pos :" + pos.toString()));
		if (!pos.isInit() && pos.getInResp() != null) {
			LoggerFactory.getLogger("MOCK").info(String.format("inresp"));
			long now = System.currentTimeMillis();
			pos = pos.getInResp().getTarget();
			lastAction = now;
			return true;
		}
		if (pos.getMaxDelay() > System.currentTimeMillis() - lastAction || pos.getMaxDelay() == 0) {
			Thread.sleep(50);
			return true;
		}
		if (!passOutResp && (pos.plannedResponse() > System.currentTimeMillis() - lastAction || pos.plannedResponse() == 0)) {
			Thread.sleep(50);
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
		return false;
	}


	/**
	 * This method calls the addHandler method or creates an output request according
	 * to the rule type.
	 * @param rules: the list of rules.
	 * @throws RuleAlreadyExistsException if the rule already exists.
	 */
	/*private void initRules(List<Rule> rules) throws RuleAlreadyExistsException {
        for (Rule rule: rules) {
            if (rule instanceof InOutRule) {
                addHandler((InOutRule) rule);
            } else if (rule instanceof OutInRule) {
                this.rules.add(rule);
                new OutputRequest((OutInRule) rule).run();//.start();  start for multi-threading
            }
        }
    }*/

	/**
	 * This method creates a handler for the given rule.
	 * @param rule: the InOut rule.
	 * @throws RuleAlreadyExistsException if the rule already exists.
	 */
	private void addHandler(InOutRule rule) throws RuleAlreadyExistsException {
		String simplePath = rule.getRequest().getPath().split("\\?")[0];
		String id = rule.getRequest().getMethod() + simplePath;
		if (handlers.containsKey(id)) {
			handlers.get(id).addRule(rule);
		} else {
			InOutHandler handler = new InOutHandler();
			handler.addRule(rule);
			try {
				app.addHandler(HandlerType.valueOf(rule.getRequest().getMethod()), simplePath, handler);
			} catch (IllegalArgumentException e) {
				throw new RuleAlreadyExistsException(String.format("The route '%s -> %s' cannot be created.",rule.getRequest().getMethod(), simplePath));
			}
			handlers.put(id, handler);
		}
	}

	private List<Rule> getRules() {
		return rules;
	}

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		app.servlet().service(req, resp);
	}
}
