package utils;

import java.io.UnsupportedEncodingException;
import java.security.SignatureException;
import java.text.ParseException;
import java.util.ArrayList;

import org.joda.time.DateTime;

public final class MaintenanceUtil {
	//Static
	private static final String UTF8 = "UTF-8";

	public static boolean checkResponse(byte[] nounce, byte[]signature, String response, DateTime cleanSchedule, ArrayList<String> nounces, EncryptionUtil encryption, EncryptionUtil svEncryption) throws UnsupportedEncodingException, ParseException, SignatureException {

		MaintenanceUtil.cleanNounces(nounces, cleanSchedule);
		//check nounce
		
		String pureNounce = new String(encryption.decrypt(nounce), UTF8);
		
		String[] parsedNounce = pureNounce.split("%");
		
		//checks if nounce doesnt exist and its still fresh
		if(!nounces.contains(pureNounce)) {
			nounces.add(pureNounce);
		}
		else {
			return false;
		}

		if(!DateUtil.checkFreshnessMinutes(DateUtil.convertDate(parsedNounce[1]), 5)) {
			return false;
		}

		//check signature
		String signatureGuess = response.concat(pureNounce);
		
		return svEncryption.verifySignature(signatureGuess.getBytes(UTF8), signature);
	}

	
	public static void cleanNounces(ArrayList<String> nounces,DateTime cleanSchedule) throws ParseException {
		if(DateUtil.checkStinkDays(cleanSchedule,2)) {
			for(int i = 0; i < nounces.size(); i++) {
				String[] parsedNounce = nounces.get(i).split("%");
				DateTime timestamp = DateUtil.convertDate(parsedNounce[1]);
				if(DateUtil.checkStinkDays(timestamp,2)) {
					nounces.remove(i);
				}
			}
		}
	}
}
