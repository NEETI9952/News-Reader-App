package com.example.newsreader;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> content=new ArrayList<>();


    private ArrayAdapter arrayAdapter;

    SQLiteDatabase techStoriesDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        techStoriesDb=this.openOrCreateDatabase("Tech Stories",MODE_PRIVATE,null);
        techStoriesDb.execSQL("CREATE TABLE IF NOT EXISTS stories(id INTEGER PRIMARY KEY, storyID INTEGER, title VARCHAR, content VARCHAR)");

        DownloadTask task= new DownloadTask();
        try{
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e){
            e.printStackTrace();
        }

        ListView listView=findViewById(R.id.listView);
        arrayAdapter=new ArrayAdapter(this, android.R.layout.simple_list_item_1,titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent=new Intent(getApplicationContext(),webView.class);
                intent.putExtra("content",content.get(position));

                startActivity(intent);
            }
        });

        updateListView();

    }

    public void updateListView(){
        Cursor c= techStoriesDb.rawQuery("SELECT * FROM stories",null);

        int contentIndex=c.getColumnIndex("content");
        int titleIndex=c.getColumnIndex("title");

        if(c.moveToFirst()){
            titles.clear();
            content.clear();

            do {
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));

            } while(c.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        }

    }

    public class DownloadTask extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... urls) {
            String result="";
            URL url;
            HttpURLConnection urlConnection;

            try {

                url= new URL(urls[0]);
                urlConnection=(HttpURLConnection) url.openConnection();
                InputStream inputStream= urlConnection.getInputStream();
                InputStreamReader inputStreamReader= new InputStreamReader(inputStream);

                int data=inputStreamReader.read();

                while(data!=-1){
                    char current= (char)data;
                    result+=current;
                    data=inputStreamReader.read();
                }
                Log.i("result",result);

                JSONArray jsonArray=new JSONArray(result);
                int numberOfStories=20;

                if(jsonArray.length()<20){
                    numberOfStories=jsonArray.length();
                    Log.i("number of stories",Integer.toString(numberOfStories));
                }

                techStoriesDb.execSQL("DELETE FROM stories");

                for(int i=0;i<numberOfStories;i++){
                    String storyID=jsonArray.getString(i);
                    url= new URL("https://hacker-news.firebaseio.com/v0/item/"+ storyID +".json?print=pretty");
                    urlConnection=(HttpURLConnection) url.openConnection();
                    inputStream= urlConnection.getInputStream();
                    inputStreamReader= new InputStreamReader(inputStream);

                    data=inputStreamReader.read();

                    String storyInfo="";

                    while(data!=-1){
                        char current= (char)data;
                        storyInfo+= current;
                        data=inputStreamReader.read();
                    }

                    Log.i("StoryInfo",storyInfo);
                    JSONObject jsonObject= new JSONObject(storyInfo);

                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")){
                        String storyTitle= jsonObject.getString("title");
                        String storyUrl=jsonObject.getString("url");

                        Log.i("Title,URl",storyTitle +"  "+ storyUrl);
                        url= new URL(storyUrl);
                        urlConnection= (HttpURLConnection) url.openConnection();
                        inputStream=urlConnection.getInputStream();
                        inputStreamReader=new InputStreamReader(inputStream);

                        data=inputStreamReader.read();
                        String storyContent="";

                        while(data!=-1){
                            char current = (char) data;
                            storyContent += current;
                            data=inputStreamReader.read();
                        }

//                        Log.i("HTML",storyContent);

                        String sql="INSERT INTO stories(storyID,title,content) VALUES (?,?,?)";
                        SQLiteStatement statement= techStoriesDb.compileStatement(sql);
                        statement.bindString(1,storyID);
                        statement.bindString(2,storyTitle);
                        statement.bindString(3,storyContent);

                        statement.execute();
                    }

                }

                Log.i("URL",result);
                return result;

            } catch (Exception e){
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }
}