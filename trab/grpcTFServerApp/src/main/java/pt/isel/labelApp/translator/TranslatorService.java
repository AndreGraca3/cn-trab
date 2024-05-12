package pt.isel.labelApp.translator;

import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;

import java.util.ArrayList;
import java.util.List;

public class TranslatorService {
    static List<String> TranslateLabels(List<String> labels) {
        Translate translateService = TranslateOptions.getDefaultInstance().getService();
        List<String> labelsTranslated = new ArrayList<>();
        for (java.lang.String label : labels) {
            Translation translation = translateService.translate(
                    label,
                    Translate.TranslateOption.sourceLanguage("en"),
                    Translate.TranslateOption.targetLanguage("pt"));
            labelsTranslated.add(translation.getTranslatedText());
        }
        return labelsTranslated;
    }
}
