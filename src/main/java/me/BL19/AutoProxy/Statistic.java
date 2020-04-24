package me.BL19.AutoProxy;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class Statistic {

	public String name;
	public double value;
	public String format;
	public double formattedNumber;
	public String formatted;
	Formatting formatting = Formatting.NUMBER;
	
	public void increase() {
		value++;
		format();
	}
	
	public void increase(double amount) {
		value += amount;
		format();
	}
	
	public void format() {
		if(formatting == Formatting.BINARY) {
			double temp = value + 0;
			int iter = 0;
			while (temp >= 1024) {
				temp /= 1024;
				iter++;
			}
			format = new String[] {"b", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"}[iter];
			formattedNumber = temp;
			DecimalFormat df = new DecimalFormat("#.####");
			df.setRoundingMode(RoundingMode.CEILING);
			formatted = df.format(temp) + " " + format;
		} else {
			formatted = value + "";
			formattedNumber = value;
		}
	}
	
	public enum Formatting {
		
		BINARY,
		NUMBER
		
	}
	
	public static void main(String[] args) {
		Statistic stat = new Statistic();
		stat.formatting = Formatting.BINARY;
		stat.increase(987654321);
		System.out.println(stat.formatted);
	}
	
}
