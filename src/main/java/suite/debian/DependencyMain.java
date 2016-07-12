package suite.debian;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import suite.debian.AptUtil.Repo;
import suite.os.FileUtil;
import suite.streamlet.Read;
import suite.util.Rethrow;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

public class DependencyMain extends ExecutableProgram {

	private DebianUtil debianUtil = new DebianUtil();
	private DpkgUtil dpkgUtil = new DpkgUtil(debianUtil);
	private AptUtil aptUtil = new AptUtil(debianUtil);

	// Tools
	private List<String> baseList = Arrays.asList( //
			"acpi" //
			, "cifs-utils" //
			, "deborphan" //
			, "grub-pc" //
			, "imagemagick" //
			, "less" //
			, "linux-headers-686-pae" //
			, "linux-image-686-pae" //
			, "manpages" //
			, "netcat-traditional" //
			, "rlwrap" //
			, "rsync" //
			, "ssh" //
			, "sshfs" //
			, "subversion" //
			, "unzip" //
			, "usbutils" // lsusb
			, "vim" //
			, "w3m" //
			, "wpasupplicant" //
			, "zip" //
	);

	private List<String> debianList = Arrays.asList( //
			"icedove" // firefox
			, "iceweasel" // thunderbird
	);

	private List<String> devList = Arrays.asList( //
			"bochs" //
			, "build-essential" //
			, "g++" //
			, "git-core" //
			, "golang" //
			, "libreadline-dev" //
			, "openjdk-8-jdk" //
	);

	private List<String> gamesList = Arrays.asList( //
			"frogatto" //
			, "gnugo" //
			, "supertux" //
			, "torcs" //
			, "xscavenger" //
	);

	private List<String> guiList = Arrays.asList( //
			"abiword" //
			, "asunder" //
			, "evince" //
			, "chromium" //
			, "compizconfig-settings-manager" //
			, "compiz-plugins" //
			, "dia" //
			, "dosbox" //
			, "fonts-droid" //
			, "fonts-inconsolata" //
			, "fonts-umeplus" //
			, "fontforge" //
			, "gcin" //
			, "gconf-editor" //
			, "gnome-tweak-tool" //
			, "gnumeric" //
			, "gparted" //
			, "gpicview" //
			, "leafpad" //
			, "libreoffice" //
			, "lightdm" //
			, "obconf" //
			, "openbox" //
			, "pcmanfm" //
			, "pidgin" //
			, "pidgin-hotkeys" //
			, "rdesktop" //
			, "rxvt-unicode" //
			, "scite" //
			, "terminator" //
			, "thunderbird" //
			, "tilda" //
			, "tint2" //
			, "unetbootin" //
			, "virtualbox-dkms" //
			, "virtualbox-qt" //
			, "wine" //
			, "wine32" //
			, "xchm" //
			, "xpdf" //
			, "xserver-xorg" //
			, "yeahconsole" //
	);

	private List<String> mediaList = Arrays.asList( //
			"alsa-utils" //
			, "flac" //
			, "mpg321" //
	);

	// Not a must, but good to have
	private List<String> supplementaryList = Arrays.asList( //
			"btrfs-tools" //
			, "eject" //
			, "gnupg2" //
			, "gstreamer1.0-plugins-good" //
			, "python-imaging" // for bitmap2ttf
	);

	private List<String> operatingSystemList = Arrays.asList( //
			"iamerican" //
			, "ibritish" //
	);

	private Set<String> requiredList = new HashSet<>(Util.add( //
			baseList //
			, debianList //
			, devList //
			, gamesList //
			, guiList //
			, mediaList //
			, operatingSystemList //
			, supplementaryList //
	));

	public static void main(String args[]) {
		Util.run(DependencyMain.class, args);
	}

	protected boolean run(String args[]) throws IOException {
		Read.from(getClass().getMethods()) //
				.filter(m -> m.getName().startsWith("list") && m.getParameters().length == 0) //
				.sink(m -> {
					System.out.println(m.getName() + "()");
					for (Object object : Rethrow.ex(() -> (List<?>) m.invoke(this, new Object[] {})))
						System.out.println(object);
					System.out.println();
					System.out.println();
				});
		return true;
	}

	public List<String> listDeinstalledPackages() {
		List<Map<String, String>> packages = dpkgUtil.readInstalledPackages();
		return Read.from(packages) //
				.filter(pm -> pm.get("Status").contains("deinstall")) //
				.map(pm -> "sudo dpkg --purge " + packageName(pm)) //
				.sort(Util::compare) //
				.toList();
	}

	public List<String> listDependeesOfDkms() {
		Repo repo = new Repo("http://mirrors.kernel.org/ubuntu" //
				, "xenial" //
				, "main" //
				, "amd64");
		String packageName = "dkms";

		List<Map<String, String>> packages;
		packages = Rethrow.ioException(() -> aptUtil.readRepoPackages(repo));
		Set<String> required = new HashSet<>(Arrays.asList(packageName));
		Set<String> required1 = dpkgUtil.getDependeeSet(packages, required);
		return Read.from(required1) //
				.map(packageName_ -> aptUtil.getDownloadUrl(repo, packages, packageName_)) //
				.sort(Util::compare) //
				.toList();
	}

	public List<String> listManuallyInstalled() {
		return aptUtil.readManuallyInstalled().toList();
	}

	public List<String> listUndependedPackages() {
		List<Map<String, String>> packages = dpkgUtil.readInstalledPackages();
		Map<String, List<String>> dependees = dpkgUtil.getDependersOf(packages);

		return Read.from(packages) //
				.filter(pm -> !isEssential(pm)) //
				.map(this::packageName) //
				.filter(packageName -> !dependees.containsKey(packageName)) //
				.filter(packageName -> !requiredList.contains(packageName)) //
				.map(packageName -> "sudo apt remove -y --purge " + packageName) //
				.sort(Util::compare) //
				.toList();
	}

	public List<String> listUnusedPackages() {
		List<Map<String, String>> packages = dpkgUtil.readInstalledPackages();
		Set<String> required = new HashSet<>(requiredList);

		required.addAll(Read.from(packages) //
				.filter(this::isEssential) //
				.map(this::packageName) //
				.toList());

		Set<String> required1 = dpkgUtil.getDependeeSet(packages, required);

		return Read.from(packages) //
				.map(this::packageName) //
				.filter(packageName -> !required1.contains(packageName)) //
				.sort(Util::compare) //
				.toList();
	}

	public List<String> listUnusedFiles() {
		Set<String> files = Read.from(dpkgUtil.readInstalledPackages()) //
				.concatMap(dpkgUtil::readFileList) //
				.toSet();

		return Read.from("/etc", "/usr") //
				.concatMap(p -> FileUtil.findPaths(Paths.get(p))) //
				.map(Path::toString) //
				.filter(p -> !files.contains(p)) //
				.toList();
	}

	private String packageName(Map<String, String> pm) {
		return pm.get("Package");
	}

	private boolean isEssential(Map<String, String> pm) {
		return Objects.equals(pm.get("Essential"), "yes") //
				|| Arrays.asList("important", "required").contains(pm.get("Priority"));
	}

}
