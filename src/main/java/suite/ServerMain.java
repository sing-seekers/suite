package suite;

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
import primal.persistent.PerList;
import primal.persistent.PerMap;
import primal.primitive.adt.Bytes;
import suite.cfg.Defaults;
import suite.http.Http;
import suite.http.Http.Handler;
import suite.http.Http.Header;
import suite.http.Http.Response;
import suite.http.HttpHandle;
import suite.http.HttpHeaderUtil;
import suite.http.HttpServe;
import suite.node.Str;
import suite.os.Execute;
import suite.os.Schedule;
import suite.os.Scheduler;
import suite.telegram.TelegramBot;
import suite.util.RunUtil;

// mvn compile exec:java -Dexec.mainClass=suite.ServerMain
public class ServerMain {

	public static void main(String[] args) {
		RunUtil.run(new ServerMain()::run);

		Execute.shell("x-www-browser http://127.0.0.1:8051/html/render.html");
		// Execute.shell("x-www-browser http://127.0.0.1:8051/site");
	}

	private boolean run() {
		Start.thread(this::runHttpServer);
		Start.thread(this::runScheduler);
		Start.thread(this::runTelegramBot);
		return true;
	}

	private void runHttpServer() {
		BiPredicate<String, String> authenticate = (username, password) -> Defaults //
				.secrets() //
				.prove(Suite.substitute("auth .0 .1", new Str(username), new Str(password)));

		var sseHeaders = new Header(PerMap //
				.<String, PerList<String>>empty() //
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

		var handler1 = HttpHandle.ofDispatch(PerMap //
				.<String, Handler>empty() //
				.put("hello", HttpHandle.ofData("Hello world")) //
				.put("html", HttpHandle.ofPath(Paths.get("src/main/html"))) //
				.put("path", HttpHandle.ofPath(Tmp.root)) //
				.put("site", HttpHandle.ofSession(authenticate, handlerSite)) //
				.put("sse", handlerSse));

		new HttpServe(8051).serve(handler1);
	}

	private void runScheduler() {
		new Scheduler(List.of( //
				Schedule.ofDaily(LocalTime.of(18, 0), () -> DailyMain.main(null)), //
				Schedule.ofRepeat(5, () -> System.out.println("." + LocalDateTime.now())), //
				Schedule.of(LocalDateTime.of(2099, 1, 1, 0, 0), ArrayList::new)) //
		).run();
	}

	private void runTelegramBot() {
		new TelegramBot().bot((userId, message) -> message);
	}

}
