package suite.util;

import java.util.concurrent.Callable;

import org.apache.log4j.Level;

import suite.os.Execute;
import suite.os.Log_;
import suite.primitive.IntMutable;
import suite.primitive.IntPrimitives.IntSource;
import suite.sample.Profiler;

public class RunUtil {

	public enum RunOption {
		RUN____, PROFILE, TIME___,
	};

	public static boolean isLinux64() {
		if (System.getenv("USE_32BIT") != null)
			return false;
		else if (System.getenv("USE_64BIT") != null)
			return true;
		else
			return RunUtil.isLinux() && Execute.shell("uname -a").contains("x86_64");
	}

	public static boolean isLinux() {
		var os = System.getenv("OS");
		return os == null || !os.startsWith("Windows");
	}

	public static void run(Callable<Boolean> callable) {
		run(RunOption.RUN____, callable);
	}

	public static void run(RunOption runOption, Callable<Boolean> callable) {
		Log_.initLogging(Level.INFO);
		var mutableCode = IntMutable.nil();

		IntSource source = () -> {
			try {
				return callable.call() ? 0 : 1;
			} catch (Throwable ex) {
				ex.printStackTrace();
				Log_.fatal(ex);
				return 2;
			}
		};

		Runnable runnable = () -> mutableCode.set(source.g());

		switch (runOption) {
		case PROFILE:
			new Profiler().profile(runnable);
			break;
		case RUN____:
			runnable.run();
			break;
		case TIME___:
			Log_.duration("main", () -> {
				runnable.run();
				return Boolean.TRUE;
			});
		}

		var code = mutableCode.value();
		if (code != 0)
			System.exit(code);
	}

}
