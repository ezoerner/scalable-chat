package scalable.client.chat.view;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.PaneBuilder;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import scalable.client.chat.views.Browser;

/**
 * Code adapted from <a href="https://github.com/frtj/javafx_examples">github.com/frtj/javafx_examples</a>
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */

public class BrowserTest extends Application {

    private static final String SMALL_CONTENT = "Lorem ipsum dolor sit amet";
    private static final String BIG_CONTENT = "Lorem ipsum dolor sit amet, consectetuer adipiscing elit, sed diam nonummy nibh euismod" +
            " tincidunt ut laoreet dolore magna aliquam erat volutpat. Ut wisi enim ad minim veniam, " +
            "quis nostrud exerci tation ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo" +
            " consequat. Duis autem vel eum iriure dolor in hendrerit in vulputate velit esse molestie" +
            " consequat, vel illum dolore eu feugiat nulla facilisis at vero eros et accumsan et iusto" +
            " odio dignissim qui blandit praesent luptatum zzril delenit augue duis dolore te feugait" +
            " nulla facilisi. Nam liber tempor cum soluta nobis eleifend option congue nihil imperdiet" +
            " doming id quod mazim placerat facer possim assum. Typi non habent claritatem insitam;" +
            " est usus legentis in iis qui facit eorum claritatem. Investigationes demonstraverunt " +
            "lectores legere me lius quod ii legunt saepius. Claritas est etiam processus dynamicus," +
            " qui sequitur mutationem consuetudium lectorum. Mirum est notare quam littera gothica," +
            " quam nunc putamus parum claram, anteposuerit litterarum formas humanitatis per seacula " +
            "quarta decima et quinta decima. Eodem modo typi, qui nunc nobis videntur parum clari, " +
            "fiant sollemnes in futurum.";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception
    {
        final Browser browser = new Browser(SMALL_CONTENT);

        final Pane pane = PaneBuilder.create().children(browser).style("-fx-border-color: blue").build();

        pane.widthProperty().addListener((observable, oldValue, newValue) -> browser.setPrefWidth((Double)newValue));

        Button button = new Button("More..");
        button.setMinWidth(Control.USE_PREF_SIZE);
        button.setOnAction(new ButtonEventHandler(browser));


        BorderPane border = new BorderPane();
        border.setTop(button);

        VBox vbox = new VBox();
        vbox.getChildren().add(pane);
        vbox.setAlignment( Pos.TOP_CENTER );
        border.setCenter(vbox);


        Scene scene = new Scene(border, 800, 600);

        stage.setScene(scene);
        stage.setTitle("Test");
        stage.show();

    }


    private class ButtonEventHandler implements EventHandler<ActionEvent> {

        private boolean toggle = false;
        private final Browser browser;

        private ButtonEventHandler(Browser browser)
        {
            this.browser = browser;
        }

        @Override
        public void handle(ActionEvent e) {
            toggle = !toggle;
            if (toggle) {
                browser.setContent(	BIG_CONTENT);

            } else {
                browser.setContent(SMALL_CONTENT);
            }
        }
    }
}