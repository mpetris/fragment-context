package eu.interedition.fragmentContext.text.urifragident;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;


public abstract class TextFragmentIdentifier {
	
	private Integer length;
	private String md5HexValue;
	private String mimeCharset;
	private Range range;
	
	protected TextFragmentIdentifier() {
		super();
	}

	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		if (length!=null) {
			this.length = length;
		}
	}

	public String getMd5HexValue() {
		return md5HexValue;
	}

	public void setMd5HexValue(String md5HexValue) {
		if (md5HexValue!=null) {
			this.md5HexValue = md5HexValue;
		}
	}

	public String getMimeCharset() {
		return mimeCharset;
	}

	public void setMimeCharset(String mimeCharset) {
		if (mimeCharset!=null) {
			this.mimeCharset = mimeCharset;
		}
	}

	public Integer getStartPos() {
		return range.getStartPos();
	}

	public Integer getEndPos() {
		return range.getEndPos();
	}
	
	public void setRange(Range range) {
		this.range = range;
	}

	public abstract String getTextFragmentFrom(String primarySource);
	
	public String getTextFragmentFrom(InputStream is) throws IOException {
		
		//TODO: checksum evaluating
		if (getMimeCharset() != null) {
			return getTextFragmentFrom(IOUtils.toString(is, getMimeCharset()));
		}

		return getTextFragmentFrom(IOUtils.toString(is));
	}
	
	@Override
	public String toString() {
		return "RANGE" + range + " LENGTH[" + getLength() + "] MD5[" + getMd5HexValue() + "] MIMECHARSET[" + getMimeCharset() + "]";  
	}

}