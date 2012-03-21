package eu.interedition.fragmentContext.text;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.math.*;

import de.tud.kom.stringmatching.shinglecloud.ShingleCloud;
import de.tud.kom.stringutils.tokenization.CharacterTokenizer;
import eu.interedition.fragmentContext.Constraint;
import eu.interedition.fragmentContext.Context;
import eu.interedition.fragmentContext.Primary;

public class TextContext implements Context {
	
	public static final int DEFAULT_CONTEXTLENGTH = 20;

	/*
	 * Percentage of any given selection that should be stored.
	 * If selection is below DEFAULT_CONTEXTLENGTH, then the entire
	 * selection is stored. Otherwise, only the given percentage below
	 * of the beginning and end of the selection is used.
	 */
	private static double percentStorage = 0.15;
	
	public static enum HashType {
		MD5, SHA, Length
	};
	
	private byte[] checkSum;

	private HashType checkSumType;

	private int totalSelectionLength;
	
	String beforeContext;

	String afterContext;
	
	String beginSel;
	
	String endSel;
	
	String totalSelection;

	public TextContext(TextPrimary primary, TextConstraint constraint,
			HashType checkSumType, int contextLength) {
		super();
		this.checkSumType = checkSumType;
		this.checkSum = checkSum(primary.getContent(), checkSumType);
		
		int beforeStart = constraint.getStartPos() - contextLength;
		beforeStart = Math.max(0, beforeStart);
		int beforeEnd = constraint.getStartPos();
		
		int afterStart = constraint.getEndPos();
		int afterEnd = constraint.getEndPos() + contextLength;
		afterEnd = Math.min(primary.getContent().length(), afterEnd);
		
		// Evaluating how much of selected text to store
		this.totalSelectionLength = primary.getContent().length();
		int cLength = this.totalSelectionLength;
		if(this.totalSelectionLength > DEFAULT_CONTEXTLENGTH) {
			double half = (double)(this.totalSelectionLength / 2);
			cLength = (int)(Math.floor(half * percentStorage));
			this.beginSel = primary.getContent().substring(beforeEnd, (beforeEnd + cLength));
			this.endSel = primary.getContent().substring((afterStart - cLength), afterStart);
			this.totalSelection = this.beginSel.concat(this.endSel);
		} else {
			// Use the entire selection
			this.beginSel = "";
			this.endSel = "";
			this.totalSelection = primary.getContent();
		}
		
		this.beforeContext = primary.getContent().substring(beforeStart, beforeEnd);
		this.afterContext = primary.getContent().substring(afterStart, afterEnd);
	}
	
	public TextContext(TextConstraint constraint, byte[] checkSum,
			HashType checkSumType, String beforeContext, String afterContext) {
		super();
		this.checkSumType = checkSumType;
		this.checkSum = checkSum;
		
		this.beforeContext = beforeContext;
		this.afterContext = afterContext;
	}

	static byte[] checkSum(String content, HashType checksumType) {
		byte[] contentBytes;
		try {
			contentBytes = content.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			throw new RuntimeException("Encoding unknown!");
		}
		MessageDigest md;
		try {
			md = MessageDigest.getInstance(checksumType.name());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			throw new RuntimeException("Algorithm unknown!");
		}
		byte[] digest = md.digest(contentBytes);

		return digest;
	}
	
	@Override
	public boolean verify(Primary primary) {
		if (!(primary instanceof TextPrimary))
			throw new IllegalArgumentException();
		TextPrimary textPrimary = (TextPrimary) primary;

		byte[] digest = checkSum(textPrimary.getContent(), this.checkSumType);
		
		
		return Arrays.equals(digest, this.checkSum);

	}

	private ShingleCloud createSC(int needleLength, String hayStack) {
		
		ShingleCloud sc = new ShingleCloud(hayStack);
		sc.setTokenizer(new CharacterTokenizer());
		
		int nGramSize = Math.min((int)(needleLength), 20);
		sc.setNGramSize(nGramSize);
		
		sc.setMinimumNumberOfOnesInMatch((int) (1));
		sc.setSortMatchesByRating(true);

		return sc;
	}
	
	private TextConstraint shingleCloudMatch(String primaryContent) throws NoMatchFoundException {
		
		ShingleCloud sc = createSC(this.beforeContext.length(), primaryContent);

		//find the text before the annotation
		sc.match(this.beforeContext);
		if (sc.getMatches().isEmpty())
			throw new Context.NoMatchFoundException();
		
		int startPos = sc.getMatches().get(0).getStart() +
			sc.getMatches().get(0).getLength();	
		
		//find text after annotation
		sc = createSC(this.afterContext.length(), primaryContent);
		
		sc.match(this.afterContext);
		if (sc.getMatches().isEmpty())
			throw new Context.NoMatchFoundException();
		
		int endPos = sc.getMatches().get(0).getStart();
		
		return new TextConstraint(startPos, endPos);
	}

	private TextConstraint exactMatch(String primaryContent, TextConstraint originalConstraint) throws NoMatchFoundException {
		
		//find the text before the annotation
		//int startPos = primaryContent.indexOf(this.beforeContext);
		int startPos = findClosestIndexOf(
				this.beforeContext, 
				originalConstraint.getStartPos() - this.beforeContext.length(), 
				primaryContent);
		
		startPos += this.beforeContext.length();
		
		//find text after annotation
//		int endPos = primaryContent.indexOf(this.afterContext);
		int endPos = findClosestIndexOf(
				this.afterContext, 
				originalConstraint.getEndPos(), 
				this.totalSelection);
		
		if (endPos < 0 || startPos < 0) {
			// search through the selected content
			
			int positionTotal = 0;
			if(this.beginSel.length() > 0 && this.endSel.length() > 0) {
				int originalPosTotal = (originalConstraint.getStartPos() 
						+ (originalConstraint.getEndPos() - this.endSel.length()));
				
				// search beginning source selection, then end 
				// selection, respectively
				int beginTotal = findClosestIndexOf(
						this.beginSel,
						originalConstraint.getStartPos(),
						primaryContent);
				
				int afterTotal = findClosestIndexOf(
						this.endSel,
						(originalConstraint.getEndPos() - this.endSel.length()),
						primaryContent);
				
				if(Math.abs(positionTotal - originalPosTotal) > 5) {
					throw new Context.NoMatchFoundException();
				}
				
			} else {
				// search through total selection
				positionTotal = findClosestIndexOf(
						this.totalSelection,
						originalConstraint.getStartPos(),
						primaryContent);
				if(positionTotal < 0)
					throw new Context.NoMatchFoundException();
				
			}
			
			
		}
		
		return new TextConstraint(startPos, endPos);
	}

	private int findClosestIndexOf(String context, int oldIndex, String content) {
	
		Matcher matcher = Pattern.compile(Pattern.quote(context)).matcher(content);
		int index = 0;
		
		while (matcher.find()) {
			if (Math.abs(oldIndex-matcher.start()) < (Math.abs(oldIndex-index))) {
				index = matcher.start();
			}
		}
		
		return index;
	}

	
	@Override
	public Constraint match(Primary primary, TextConstraint originalConstraint) throws Context.NoMatchFoundException {
		if (!(primary instanceof TextPrimary))
			throw new IllegalArgumentException();
		TextPrimary textPrimary = (TextPrimary) primary;
		
		//TextConstraint result = shingleCloudMatch(textPrimary.getContent());
		TextConstraint result = exactMatch(textPrimary.getContent(), originalConstraint);
		
		//sanity check
		if (result.getEndPos() < result.getStartPos())
			throw new Context.NoMatchFoundException();
			
		return result;
	}

	public byte[] getCheckSum() {
		return checkSum;
	}
	
	public String getBeforeContext() {
		return beforeContext;
	}
	
	public String getAfterContext() {
		return afterContext;
	}
}
