package me.BL19.AutoProxy.Utils;

public class ArrayUtils {

	public static String[] remove(String[] array, int index) {
		String[] a = new String[array.length - index];
		for (int i = 0; i < a.length; i++) {
			a[i] = array[i + index];
		}
		return a;
	}
	
	public static String[] getLast(int last, String[] arr) {
		if(last > arr.length) last = arr.length;
		String[] res = new String[last];
		for (int i = 0; i < last; i++) {
			res[i] = arr[arr.length - last + i];
		}
		return res;
	}
	
}
