package app.cap.beshopserver;





import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;


public class rExt {


    public static SecretKey generateKey(char[] passphraseOrPin, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {

        //PBKDF2 경화 라운드 횟수, 큰 값 증가 시간
        //계산을 유발하는 값을 선택
        //최대 100ms 넘게 걸릴 수 있음

        final int iterations = 1000;

        // Generate a 256-bit key
        final int outputKeyLength = 177;

        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        KeySpec keySpec = new PBEKeySpec(passphraseOrPin, salt, iterations, outputKeyLength);
        SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);

        return secretKey;
    }

}
