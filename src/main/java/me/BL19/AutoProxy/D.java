package me.BL19.AutoProxy;

public class D {

	public static boolean isDebug(String trace) {
		if(AutoProxy.debugTrace == null) return false;
		String[] chars = AutoProxy.debugTrace.split("");
		String[] tchars = trace.split("");
		int tcharsOff = 0;
		for (int i = 0; i < chars.length; i++) {
			String s = chars[i];
//			System.out.println(s + " - " + tchars[i + tcharsOff] + " (" + tcharsOff + ")");
			if(s.equals("*")) {
				if(i != chars.length - 1) {
					String cNext = chars[i + 1];
					for (int j = i; j < tchars.length; j++) {
						String tC = tchars[j];
						if(tC.equals(cNext) && tchars[j + 1].equals(chars[i + 2])) {
							tcharsOff = j - i - 1;
							break;
						}
					}
					continue;
				} else {
					return true;
				}
			}
			if(!s.equals(tchars[i + tcharsOff])) {
				return false;
			}
		}
		return true;
	}
	
	public static void main(String[] args) {
		AutoProxy.debugTrace = "AP.*.Info";
		System.out.println(AutoProxy.debugTrace);
		test("AP.HTTP.Headers");
		test("AP.HTTP.File.Download");
		test("AP.HTTP.File.Info");
		test("AP.HTTP.Html.Info");
		test("AP.Start.Info");

	}

	private static void test(String string) {
		System.out.println(string + ": " + isDebug(string));
		
	}
	
}
