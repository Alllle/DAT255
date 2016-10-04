package gruppn.kasslr;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import gruppn.kasslr.model.Vocabulary;
import gruppn.kasslr.model.VocabularyItem;

public class VocabularyFeedAdapter extends RecyclerView.Adapter<VocabularyFeedViewHolder>{

    private List<Vocabulary> vocabularies;
    private Activity activity;


    public VocabularyFeedAdapter(Activity activity, List<Vocabulary> vocabularies){
        this.vocabularies = vocabularies;
        this.activity = activity;
    }

    @Override
    public int getItemCount(){
        return vocabularies.size();
    }

    @Override
    public VocabularyFeedViewHolder onCreateViewHolder(ViewGroup parent, int viewType){
        View itemView = LayoutInflater
                .from(parent.getContext())
                .inflate(R.layout.vocabulary_feed_card,parent,false);
        return new VocabularyFeedViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(VocabularyFeedViewHolder holder, int position){
        updateView(holder, position);
    }

    private void updateView(VocabularyFeedViewHolder holder, int position){

        Vocabulary v = vocabularies.get(position);
        List<VocabularyItem> items = v.getItems();

        setPictures(holder, items);
        holder.setName(v.getTitle());


    }

    private void setPictures(VocabularyFeedViewHolder holder, List<VocabularyItem> items){

        if(items != null) {

            Bitmap[] bitmaps = new Bitmap[3];

            if (items.size() > 2) {
                bitmaps[2] = createBitmap(items.get(2).getImageUrl());
            }
            if (items.size() > 1) {
                bitmaps[1] = createBitmap(items.get(1).getImageUrl());
            }
            if (items.size() > 0) {
                bitmaps[0] = createBitmap(items.get(0).getImageUrl());
            } else {
                throw new IllegalStateException("No items in vocabulary");
            }

            holder.setPictures(bitmaps);
        }

    }

    private Bitmap createBitmap(String imgUrl){
        Bitmap bm = BitmapFactory.decodeFile(imgUrl);
        return bm;
    }
}
