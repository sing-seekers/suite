package suite.parser;

import java.util.Set;

import suite.util.FunUtil.Fun;
import suite.util.ParseUtil;

/**
 * Remove comments.
 * 
 * @author ywsing
 */
public class CommentPreprocessor implements Fun<String, String> {

	public final static String openGroupComment = "-=";
	public final static String closeGroupComment = "=-";
	public final static String openLineComment = "--";
	public final static String closeLineComment = "\n";

	private Set<Character> whitespaces;

	public CommentPreprocessor(Set<Character> whitespaces) {
		this.whitespaces = whitespaces;
	}

	@Override
	public String apply(String in) {
		in = removeComments(in, openGroupComment, closeGroupComment);
		in = removeComments(in, openLineComment, closeLineComment);
		return in;
	}

	private String removeComments(String in, String open, String close) {
		int closeLength = !isWhitespaces(close) ? close.length() : 0;
		int start = 0;
		StringBuilder sb = new StringBuilder();

		while (true) {
			int pos0 = ParseUtil.search(in, start, open);
			if (pos0 == -1)
				break;
			int pos1 = ParseUtil.search(in, pos0 + open.length(), close);
			if (pos1 == -1)
				break;
			sb.append(in.substring(start, pos0));
			start = pos1 + closeLength;
		}

		sb.append(in.substring(start));
		return sb.toString();
	}

	private boolean isWhitespaces(String in) {
		boolean result = true;
		for (char ch : in.toCharArray())
			result &= whitespaces.contains(ch);
		return result;
	}

}
