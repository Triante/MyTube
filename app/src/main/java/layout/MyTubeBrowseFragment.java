package layout;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.triante.mytube.MainActivity;
import com.example.triante.mytube.VideoPlayerActivity;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Thumbnail;


import com.example.triante.mytube.R;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;


/**
 * Referenced from:
 * https://developers.google.com/youtube/v3/code_samples/java#search_by_keyword
 */

public class MyTubeBrowseFragment extends Fragment {

    //private static long MAX_VIDEO_LIST = 20;
    private EditText searchBar;
    private TextView results;
    private LinearLayout list;
    public MyTubeBrowseFragment() {
        // Required empty public constructor
    }

    public static MyTubeBrowseFragment newInstance() {

        MyTubeBrowseFragment fragment = new MyTubeBrowseFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v =  inflater.inflate(R.layout.fragment_my_tube_browse, container, false);
        searchBar = (EditText) v.findViewById(R.id.browseBar);
        searchBar.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    main();
                    return true;
                }
                return false;
            }
        });
        results = (TextView) v.findViewById(R.id.results);
        list = (LinearLayout) v.findViewById(R.id.videoList);
        return v;
    }

    /**
     * Define a global variable that identifies the name of a file that
     * contains the developer's API key.
     */
    private final String PROPERTIES_FILENAME = "youtube.properties";

    private final long NUMBER_OF_VIDEOS_RETURNED = 25;

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private YouTube youtube;
    private String queryTerm;
    /**
     * Initialize a YouTube object to search for videos on YouTube. Then
     * display the name and thumbnail image of each video in the result set.
     */
    public void main() {
        if (searchBar.getText().toString().trim().isEmpty()) return;
        try {
            // This object is used to make YouTube Data API requests. The last
            // argument is required, but since we don't need anything
            // initialized when the HttpRequest is initialized, we override
            // the interface and provide a no-op function.
            youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(), new HttpRequestInitializer() {
                public void initialize(HttpRequest request) throws IOException {
                }
            }).setApplicationName("MyTube").build();

            // Prompt the user to enter a query term.
            queryTerm = getInputQuery();

            // Define the API request for retrieving search results.
            YouTube.Search.List search = youtube.search().list("id,snippet");

            // Set your developer key from the Google Developers Console for
            // non-authenticated requests. See:
            // https://console.developers.google.com/
            String apiKey = MainActivity.API_KEY;
            search.setKey(apiKey);
            search.setQ(queryTerm);

            // Restrict the search results to only include videos. See:
            // https://developers.google.com/youtube/v3/docs/search/list#type
            search.setType("video");

            // To increase efficiency, only retrieve the fields that the
            // application uses.
            search.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url,snippet/publishedAt,snippet/description)");
            search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

            // Call the API and print results.
            new BrowseAsyncTask().execute(search);
        } catch (GoogleJsonResponseException e) {
            System.err.println("There was a service error: " + e.getDetails().getCode() + " : "
                    + e.getDetails().getMessage());
        } catch (IOException e) {
            System.err.println("There was an IO error: " + e.getCause() + " : " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /*
     * Prompt the user to enter a query term and return the user-specified term.
     */
    private String getInputQuery() {

        String inputQuery = searchBar.getText().toString().trim();
        if (inputQuery.length() < 1) {
            // Use the string "YouTube Developers Live" as a default.
            inputQuery = "YouTube Developers Live";
        }
        return inputQuery;
    }

    /*
     * Prints out all results in the Iterator. For each result, print the
     * title, video ID, and thumbnail.
     *
     * @param iteratorSearchResults Iterator of SearchResults to print
     *
     * @param query Search query (String)
     */
    private void prettyPrint(Iterator<SearchResult> iteratorSearchResults, String query) {

        list.removeAllViews();
        PopulateAsyncTask task = new PopulateAsyncTask(iteratorSearchResults, query);
        task.execute();
    }

    /**
     * AsyncTask to download the search request in background
     */
    private class BrowseAsyncTask extends AsyncTask<YouTube.Search.List, Void, List<SearchResult>> {

        @Override
        protected List<SearchResult> doInBackground(YouTube.Search.List... params) {
            SearchListResponse searchResponse = null;
            try {
                searchResponse = params[0].execute();
            } catch (IOException e) {
                e.printStackTrace();
            }
            List<SearchResult> searchResultList = searchResponse.getItems();
            return searchResultList;
        }

        @Override
        protected void onPostExecute(List<SearchResult> searchResultList) {
            if (searchResultList != null) {
                prettyPrint(searchResultList.iterator(), queryTerm);
            }
        }
    }

    /**
     * AsyncTask to fill up the list view and assign onTouchListeners to thumbnails in background
     */
    private class PopulateAsyncTask extends AsyncTask<Void, Void, Void> {

        private Iterator<SearchResult> resultList;
        private String search;

        PopulateAsyncTask(Iterator<SearchResult> iteratorSearchResults, String query) {
            resultList = iteratorSearchResults;
            search = query;
        }

        @Override
        protected Void doInBackground(Void... params) {
            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            results.setText("\n==========================\n");
            results.append(
                    "First " + NUMBER_OF_VIDEOS_RETURNED + " videos for search on \"" + search + "\".");
            results.append("\n==========================\n");

            if (!resultList.hasNext()) {
                results.append(" There aren't any results for your query.");
            }

            while (resultList.hasNext()) {

                final SearchResult singleVideo = resultList.next();
                final ResourceId rId = singleVideo.getId();
                // Confirm that the result represents a video. Otherwise, the
                // item will not contain a video ID.
                if (rId.getKind().equals("youtube#video")) {

                    Thumbnail thumbnail = singleVideo.getSnippet().getThumbnails().getDefault();

//                    results.append("\nVideo Id" + rId.getVideoId());
//                    results.append("\nTitle: " + singleVideo.getSnippet().getTitle());
//                    results.append("\nPublished: " + singleVideo.getSnippet().getPublishedAt());
//                    results.append("\nThumbnail: " + thumbnail.getUrl());
//                    results.append("\n-------------------------------------------------------------");

                    LinearLayout preview = new LinearLayout(getContext());
                    preview.setOrientation(LinearLayout.HORIZONTAL);
                    ImageView image = new ImageView(getContext());
                    image.setLayoutParams(new ViewGroup.LayoutParams(425, 425));
                    ImageLoadTask task = new ImageLoadTask(thumbnail.getUrl(), image);
                    task.execute();
                    preview.addView(image);

                    LinearLayout textBox = new LinearLayout(getContext());
                    textBox.setOrientation(LinearLayout.VERTICAL);
                    TextView title = new TextView(getContext());
                    title.setText(singleVideo.getSnippet().getTitle());
                    title.setTextSize(25);
                    TextView subText = new TextView(getContext());
                    subText.setText("Published: " + singleVideo.getSnippet().getPublishedAt());
                    subText.setTextSize(15);
                    textBox.addView(title);
                    textBox.addView(subText);
                    preview.addView(textBox);

                    image.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {

                            Intent toPlayer = new Intent(getContext(), VideoPlayerActivity.class);
                            toPlayer.putExtra(VIDEO_ID, rId.getVideoId());
                            toPlayer.putExtra(VIDEO_TITLE, singleVideo.getSnippet().getTitle() );
                            toPlayer.putExtra(VIDEO_DESCRIPTION, singleVideo.getSnippet().getDescription());
                            startActivity(toPlayer);
                            return false;
                        }
                    });

                    list.addView(preview);
                }
            }
        }
    }

    public static String VIDEO_ID = "VIDEO_ID";
    public static String VIDEO_TITLE = "VIDEO_TITLE";
    public static String VIDEO_DESCRIPTION = "VIDEO_DESCRIPTION";

    /**
     * AsyncTask to download and set the images to thumbnails based off a URL in background
     */
    private class ImageLoadTask extends AsyncTask<Void, Void, Bitmap> {

        private String url;
        private ImageView imageView;

        public ImageLoadTask(String url, ImageView imageView) {
            this.url = url;
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                URL urlConnection = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlConnection
                        .openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                Bitmap myBitmap = BitmapFactory.decodeStream(input);
                return myBitmap;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            super.onPostExecute(result);
            imageView.setImageBitmap(result);
        }

    }

}
