package app.cap.beshopserver;



import java.math.BigInteger;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class pProtocol {

    //변수

    private BigInteger prime;

    public final String Sk;
    public final String IDp;
    public final String IDc;
    public final String N;
    public final String T;

    public pProtocol(final String Sk, final String N, final String T, final String IDc, final String IDp){
        this.Sk = Sk;
        this.IDp = IDp;
        this.IDc = IDc;
        this.N = N;
        this.T = T;
    }



    public static byte[] xor(final byte[] input, final byte[] secret) {
        final byte[] output = new byte[input.length];
        if (secret.length == 0) {
            throw new IllegalArgumentException("empty security key");
        }
        int spos = 0;
        for (int pos = 0; pos < input.length; ++pos) {
            output[pos] = (byte) (input[pos] ^ secret[spos]);
            ++spos;
            if (spos >= secret.length) {
                spos = 0;
            }
        }
        return output;
    }
    public static String hmacSha1(String key, String value) {
        try {
            // 원시 키 바이트에서 hmac_sha1 키 가져 오기
            byte[] keyBytes = key.getBytes();
            SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");

            // hmac_sha1 Mac 인스턴스를 가져 와서 서명 키로 초기화
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(signingKey);

            // 입력 데이터 바이트에서 hmac 계산
            byte[] rawHmac = mac.doFinal(value.getBytes());

            // 원시 바이트를 16 진수로 변환
            //byte[] hexBytes = new Hex().encode(rawHmac);

            //  Hex 바이트를 문자열로 변환
            return new String(rawHmac, "UTF-8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] encrypt(byte[] raw, byte[] clear) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(clear);
            return encrypted;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public static byte[] decrypt(byte[] raw, byte[] encrypted) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] decrypted = cipher.doFinal(encrypted);
            return decrypted;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }


}
