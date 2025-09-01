package Utils;

import java.util.List;
import java.util.Random;

public class RandomKeyGeneration {

    private static final Random random = new Random();

    private static List<Alphabet> alps = List.of(new Alphabet('!', '}'),
            new Alphabet('а', 'я'), new Alphabet('А', 'Я'), new Alphabet((char) 128, (char) 1023));

    public static String getKey(int length) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            Alphabet alphabet = alps.get(random.nextInt(alps.size()));
            result.append(alphabet.getRandomChar());
        }
        return result.toString();
    }

    public static void setAlps(List<Alphabet> alps) {
        RandomKeyGeneration.alps = alps;
    }

    record Alphabet(char first, char last) {

        public char getRandomChar() {
            return (char) random.nextInt(first, last);
        }

    }
}
