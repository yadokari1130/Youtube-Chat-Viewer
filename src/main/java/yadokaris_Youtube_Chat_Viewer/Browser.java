package yadokaris_Youtube_Chat_Viewer;

public enum Browser {
	CHROME,
	EDGE,
	OPERA,
	VIVALDI,
	BRAVE,
	FIREFOX,
	;

	public static Browser getBrowser(String name) throws IllegalArgumentException {
		switch (name.toLowerCase()) {
		case "chrome":
			return CHROME;
		case "edge":
			return EDGE;
		case "opera":
			return OPERA;
		case "vivaldi":
			return VIVALDI;
		case "brave":
			return BRAVE;
		case "firefox":
			return FIREFOX;
		default:
			throw new IllegalArgumentException();
		}
	}
}
