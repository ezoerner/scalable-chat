package scalable.client.chat.views;

import java.util.Set;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Worker.State;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSException;

/**
 * Code adapted from <a href="https://github.com/frtj/javafx_examples">github.com/frtj/javafx_examples</a>
 *
 * @author Eric Zoerner <a href="mailto:eric.zoerner@gmail.com">eric.zoerner@gmail.com</a>
 */
@SuppressWarnings("MagicNumber")
public class Browser extends Region {
    public static final String CONTENT_ID = "browsercontent";

    final WebView webview = new WebView();
    final WebEngine webEngine = webview.getEngine();


    public Browser( String content ) {

        webview.setPrefHeight(5);

        setPadding(new Insets(20));

        widthProperty().addListener( new ChangeListener<Object>() {
            @Override
            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue)
            {
                Double width = (Double)newValue;
                webview.setPrefWidth(width);
                adjustHeight();
            }
        });

        webEngine.getLoadWorker().stateProperty().addListener((arg0, oldState, newState) -> {
            if (newState == State.SUCCEEDED) {
                adjustHeight();
            }
        });

        // http://stackoverflow.com/questions/11206942/how-to-hide-scrollbars-in-the-javafx-webview
        webview.getChildrenUnmodifiable().addListener(new ListChangeListener<Node>() {
            @Override public void onChanged(Change<? extends Node> change) {
                Set<Node> scrolls = webview.lookupAll(".scroll-bar");
                for (Node scroll : scrolls) {
                    scroll.setVisible(false);
                }
            }
        });

        setContent( content );

        getChildren().add(webview);
    }

    public void setContent( final String content )
    {
        Platform.runLater(() -> {
            webEngine.loadContent(getHtml(content));
            Platform.runLater(this::adjustHeight);
        });
    }


    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        layoutInArea(webview,0,0,w,h,0, HPos.CENTER, VPos.CENTER);
    }

    private void adjustHeight() {

        Platform.runLater(() -> {
            try {
                Object result = webEngine.executeScript(
                        "document.getElementById('" + CONTENT_ID + "').offsetHeight");
                if (result instanceof Integer) {
                    Integer i = (Integer) result;
                    double height = (double)i;
                    height += 20;
                    webview.setPrefHeight(height);
                }
            } catch (JSException ignored) {
                // not important
            }
        });

    }

    public String getHtml(String content) {
        return  "<html><body>" +
                "<div id=\"" + CONTENT_ID + "\">" + content + "</div>" +
                "</body></html>";
    }
}