package nicelee.bilibili.parsers;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nicelee.bilibili.PackageScanLoader;
import nicelee.bilibili.annotations.Bilibili;
import nicelee.bilibili.model.VideoInfo;
import nicelee.bilibili.util.HttpRequestUtil;
import nicelee.bilibili.util.Logger;

public class InputParser implements IInputParser, IParamSetter {

	protected final static Pattern paramPattern = Pattern.compile("^(.*)p=([0-9]+)$");// 自定义参数, 目前只匹配个人主页视频的页码
	private List<IInputParser> parsers = null;
	private IInputParser parser = null;
	private int page = 1;
	private int realQN = 1;

	public InputParser(HttpRequestUtil util, int pageSize, String loadContition) {
		parsers = new ArrayList<>();
		try {
			for (Class<?> clazz : PackageScanLoader.validParserClasses) {
				// 判断是否需要载入
				Bilibili bili = clazz.getAnnotation(Bilibili.class);
				if (bili.ifLoad().isEmpty() || bili.ifLoad().equals(loadContition)) {
					// 实例化并加入parser列表
					// IInputParser inputParser = (IInputParser) clazz.newInstance();
					// 获取构造函数
					// Constructor<IInputParser> con = (Constructor<IInputParser>)
					// clazz.getConstructor(Object[].class);
					Constructor<IInputParser> con = (Constructor<IInputParser>) clazz.getConstructors()[0];
					IInputParser inputParser = con.newInstance(new Object[] { new Object[] { util, this, pageSize } });
					parsers.add(inputParser);
				}

			}
			// 按权重排序,越大越优先
			Collections.sort(parsers, new Comparator<Object>() {
				@Override
				public int compare(Object o1, Object o2) {
					int bili1 = o1 == null? 0 : o1.getClass().getAnnotation(Bilibili.class).weight();
					int bili2 = o2 == null? 0 : o2.getClass().getAnnotation(Bilibili.class).weight();
					return bili2 - bili1;
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void selectParser(String input) {
		for (IInputParser parser : parsers) {
			if (parser.matches(input)) {
				//Logger.println(input);
				this.parser = parser;
				break;
			}
		}
	}

	@Override
	public boolean matches(String input) {
		return true;
	}

	@Override
	public String validStr(String input) {
		// 获取参数, 并去掉参数字符串
		Matcher paramMatcher = paramPattern.matcher(input);
		if (paramMatcher.find()) {
			int page = Integer.parseInt(paramMatcher.group(2));
			this.page = page;
			input = paramMatcher.group(1);
		}
		selectParser(input);
		if (parser != null) {
			return parser.validStr(input);
		}
		Logger.println("当前没有parser匹配");
		return "";
	}

	@Override
	public VideoInfo result(String input, int videoFormat, boolean getVideoLink) {
		// 获取参数, 并去掉参数字符串
		Matcher paramMatcher = paramPattern.matcher(input);
		if (paramMatcher.find()) {
			int page = Integer.parseInt(paramMatcher.group(2));
			this.page = page;
			input = paramMatcher.group(1);
		}
		selectParser(input);
		if (parser != null) {
			return parser.result(input, videoFormat, getVideoLink);
		}
		return null;
	}

	@Override
	public String getVideoLink(String avId, String cid, int qn, int downFormat) {
		selectParser(avId);
		if (parser != null) {
			return parser.getVideoLink(avId, cid, qn, downFormat);
		}
		return null;
	}

	@Override
	public int getVideoLinkQN() {
		if (parser != null) {
			return parser.getVideoLinkQN();
		}
		return 0;
	}

	@Override
	public void setPage(int page) {
		this.page = page;
	}

	@Override
	public int getPage() {
		return page;
	}

	@Override
	public void setRealQN(int qn) {
		realQN = qn;

	}

	@Override
	public int getRealQN() {
		return realQN;
	}

}
