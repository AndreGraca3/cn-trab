package pt.isel.translator;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

import java.util.ArrayList;
import java.util.List;

public class TranslatorService {

    private static final String sourceLanguage = "en";
    private static final String targetLanguage = "pt";

    public static List<String> translate(List<String> labels) {
        List<String> labelsTranslated = new ArrayList<>();
        for (String label : labels) {
            labelsTranslated.add(translate(label));
        }
        return labelsTranslated;
    }

    public static String translate(String label) {
        Translate translateService = TranslateOptions.getDefaultInstance().getService();
        Translation translation = translateService.translate(
                label,
                Translate.TranslateOption.sourceLanguage(sourceLanguage),
                Translate.TranslateOption.targetLanguage(targetLanguage)
        );
        return translation.getTranslatedText();
    }
}
