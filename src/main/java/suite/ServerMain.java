package suite;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import primal.MoreVerbs.Pull;
import primal.Nouns.Tmp;
import primal.Nouns.Utf8;
import primal.Verbs.Sleep;
import primal.Verbs.Start;
import primal.fp.Funs2.Fun2;
import primal.persistent.PerList;
import primal.persistent.PerMap;
import primal.primitive.adt.Bytes;
import suite.cfg.Defaults;
import suite.http.Http;
import suite.http.Http.Handler;
import suite.http.Http.Header;
import suite.http.Http.Response;
import suite.http.HttpAuthToken;
import suite.http.HttpHandle;
import suite.http.HttpHeaderUtil;
import suite.http.HttpNio;
import suite.http.HttpServe;
import suite.node.Str;
import suite.os.Execute;
import suite.os.FileUtil;
import suite.os.Schedule;
import suite.os.Scheduler;
import suite.sample.TelegramBotMain;
import suite.util.RunUtil;

// mvn compile exec:java -Dexec.mainClass=suite.ServerMain
public class ServerMain {

	public static void main(String[] args) {
		RunUtil.run(new ServerMain()::run);

		Execute.shell("x-www-browser http://127.0.0.1:8051/html/render.html");
		// Execute.shell("x-www-browser http://127.0.0.1:8051/site");
	}

	public boolean run() {
		Start.thread(Boolean.TRUE ? this::runNioHttpServer : this::runHttpServer);
		Start.thread(this::runScheduler);
		Start.thread(this::runTelegramBot);
		return true;
	}

	private void runHttpServer() {
		new HttpServe(8051).serve(handler());
	}

	private void runNioHttpServer() {
		new HttpNio().run(8051, handler());
	}

	private Handler handler() {
		BiPredicate<String, String> authenticate = (username, password) -> Defaults //
				.secrets() //
				.prove(Suite.substitute("auth .0 .1", new Str(username), new Str(password)));

		Fun2<String, String, List<String>> authenticateRoles = (username, password) -> {
			return authenticate.test(username, password) ? List.of("role") : null;
		};

		var sseHeaders = new Header(PerMap //
				.<String, PerList<String>> empty() //
				.put("Cache-Control", PerList.of("no-cache")) //
				.put("Content-Type", PerList.of("text/event-stream")));

		Handler handlerSite = request -> Response.of(Pull.from("" //
				+ "<html>" //
				+ "<br/>method = " + request.method //
				+ "<br/>server = " + request.server //
				+ "<br/>paths = " + request.paths //
				+ "<br/>attrs = " + HttpHeaderUtil.getAttrs(request.query) //
				+ "<br/>headers = " + request.headers //
				+ "</html>"));

		Handler handlerSse = request -> Response.ofWriter(Http.S200, sseHeaders, writer -> {
			for (var i = 0; i < 8; i++) {
				Sleep.quietly(1000l);
				var event = "event: number\ndata: { \"i\": " + i + " }\n\n";
				writer.f(Bytes.of(event.getBytes(Utf8.charset)));
			}
			writer.f(null);
		});

		var hat = new HttpAuthToken();
		var hh = new HttpHandle();

		return hh.dispatchPath(PerMap //
				.<String, Handler> empty() //
				.put("api", hat.handleFilter("role", hh.data("in good shape"))) //
				.put("hello", hh.data("Hello world")) //
				.put("html", hh.dir(Paths.get(FileUtil.suiteDir() + "/src/main/html"))) //
				.put("path", hh.dir(Tmp.root)) //
				.put("site", hh.session(authenticate, handlerSite)) //
				.put("sse", handlerSse) //
				.put("token", hh.dispatchMethod(PerMap //
						.<String, Handler> empty() //
						.put("PATCH", hat.handleRefreshToken(authenticateRoles)) //
						.put("POST", hat.handleGetToken(authenticateRoles)))));
	}

	private void runScheduler() {
		new Scheduler(List.of( //
				Schedule.ofDaily(LocalTime.of(18, 0), () -> DailyMain.main(null)), //
				Schedule.ofRepeat(5, () -> System.out.println("." + LocalDateTime.now())), //
				Schedule.of(LocalDateTime.of(2099, 1, 1, 0, 0), ArrayList::new)) //
		).run();
	}

	private boolean runTelegramBot() {
		var tokenPath = Tmp.path("kowloonbot.token");
		var b = Files.exists(tokenPath);
		if (b)
			new TelegramBotMain("Kowloonbot", tokenPath).run();
		return b;
	}

}
