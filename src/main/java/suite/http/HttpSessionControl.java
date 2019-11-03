package suite.http;

import java.security.SecureRandom;
import java.util.Random;
import java.util.function.BiPredicate;

import primal.MoreVerbs.Pull;
import primal.Verbs.Build;
import primal.Verbs.Equals;
import primal.persistent.PerList;
import primal.primitive.adt.LngMutable;
import suite.util.HtmlUtil;

/**
 * Cookie-based HTTP authentication.
 *
 * @author ywsing
 */
public class HttpSessionControl {

	public static long TIMEOUTDURATION = 3600 * 1000l;

	private BiPredicate<String, String> authenticate;
	private HtmlUtil htmlUtil = new HtmlUtil();
	private SessionManager sessionManager = new HttpSessionManager();
	private Random random = new SecureRandom();

	public interface SessionManager {
		public Session get(String id);

		public void put(String id, Session session);

		public void remove(String id);
	}

	public class Session {
		public final String username;
		public final LngMutable lastRequestDt;

		private Session(String username, long current) {
			this.username = username;
			lastRequestDt = LngMutable.of(current);
		}
	}

	public HttpSessionControl(BiPredicate<String, String> authenticate) {
		this.authenticate = authenticate;
	}

	public HttpSessionHandler getSessionHandler(HttpHandler handler) {
		return new HttpSessionHandler(handler);

	}

	private class HttpSessionHandler implements HttpHandler {
		private HttpHandler protectedHandler;

		public HttpSessionHandler(HttpHandler protectedHandler) {
			this.protectedHandler = protectedHandler;
		}

		public HttpResponse handle(HttpRequest request) {
			var current = System.currentTimeMillis();
			var cookie = request.headers.get("Cookie");
			var sessionId = cookie != null ? HttpHeaderUtil.getCookieAttrs(cookie).get("session") : null;
			var session = sessionId != null ? sessionManager.get(sessionId) : null;
			HttpResponse response;

			if (Equals.ab(request.paths, PerList.of("login"))) {
				var attrs = HttpHeaderUtil.getPostedAttrs(request.inputStream);
				var username = attrs.get("username");
				var password = attrs.get("password");
				var paths = HttpHeaderUtil.getPaths(attrs.get("path"));

				if (authenticate.test(username, password)) {
					sessionManager.put(sessionId = generateRandomSessionId(), session = new Session(username, current));

					var request1 = new HttpRequest( //
							request.method, //
							request.server, //
							paths, //
							request.query, //
							request.headers, //
							request.inputStream);

					response = showProtectedPage(request1, sessionId);
				} else
					response = showLoginPage(paths, true);
			} else if (Equals.ab(request.paths, PerList.of("logout"))) {
				if (sessionId != null)
					sessionManager.remove(sessionId);

				response = showLoginPage(PerList.end(), false);
			} else if (session != null && current < session.lastRequestDt.value() + TIMEOUTDURATION) {
				session.lastRequestDt.update(current);
				response = showProtectedPage(request, sessionId);
			} else
				response = showLoginPage(request.paths, false);

			return response;
		}

		private HttpResponse showProtectedPage(HttpRequest request, String sessionId) {
			var r = protectedHandler.handle(request);
			var headers1 = r.headers.put("Set-Cookie", "session=" + sessionId + "; Path=/site");
			return new HttpResponse(r.status, headers1, r.out);
		}

		private HttpResponse showLoginPage(PerList<String> redirectPath, boolean isLoginFailed) {
			var redirectPath1 = redirectPath.streamlet().map(p -> "/" + p).toJoinedString();

			return HttpResponse.of(Pull.from("<html>" //
					+ "<head><title>Login</title></head>" //
					+ "<body>" //
					+ "<font face=\"Monospac821 BT,Monaco,Consolas\">" //
					+ (isLoginFailed ? "<b>LOGIN FAILED</b><p/>" : "") //
					+ "<form name=\"login\" action=\"login\" method=\"post\">" //
					+ "Username <input type=\"text\" name=\"username\" autofocus /><br/>" //
					+ "Password <input type=\"password\" name=\"password\" /><br/>" //
					+ "<input type=\"hidden\" name=\"path\" value=\"" + htmlUtil.encode(redirectPath1) + "\" />" //
					+ "<input type=\"submit\" value=\"Login\">" //
					+ "</form>" //
					+ "</font>" //
					+ "</body>" //
					+ "</html>"));
		}
	}

	private String generateRandomSessionId() {
		var bytes = new byte[16];
		random.nextBytes(bytes);

		return Build.string(sb -> {
			for (var b : bytes)
				sb.append(String.format("%02x", b));
		});
	}

}
