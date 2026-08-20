package com.raf.zuhoo.ui.kb;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.response.KbArticleResponse;
import com.raf.zuhoo.databinding.ItemKbArticleBinding;

import java.util.List;

public class KbArticleAdapter extends RecyclerView.Adapter<KbArticleAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(KbArticleResponse article);
    }

    private final List<KbArticleResponse> articles;
    private final OnItemClickListener listener;

    public KbArticleAdapter(List<KbArticleResponse> articles, OnItemClickListener listener) {
        this.articles = articles;
        this.listener = listener;
    }

    public void submitList(List<KbArticleResponse> newArticles) {
        articles.clear();
        articles.addAll(newArticles);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemKbArticleBinding binding = ItemKbArticleBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(articles.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return articles.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemKbArticleBinding binding;

        ViewHolder(ItemKbArticleBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(KbArticleResponse article, OnItemClickListener listener) {

            binding.itemTitle.setText(article.getTitle());
            binding.itemSummary.setText(article.getSummary());
            binding.itemCategory.setText(article.getCategoryName());

            binding.getRoot().setOnClickListener(v -> listener.onClick(article));
        }
    }
}
