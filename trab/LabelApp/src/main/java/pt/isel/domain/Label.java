package pt.isel.domain;

public class Label {

        private String value;
        private String translation;

        public Label(String value, String translation) {
            this.value = value;
            this.translation = translation;
        }

        public String getValue() {
            return value;
        }

        public String getTranslation() {
            return translation;
        }
}
