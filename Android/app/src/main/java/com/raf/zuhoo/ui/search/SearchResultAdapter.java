package com.raf.zuhoo.ui.search;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.raf.zuhoo.data.model.response.SearchResultItem;
import com.raf.zuhoo.databinding.ItemSearchResultBinding;

import java.util.List;

public class SearchResultAdapter extends RecyclerView.Adapter<SearchResultAdapter.ViewHolder> {

    private final List<SearchResultItem> results;

    public SearchResultAdapter(List<SearchResultItem> results) {
        this.results = results;
    }

    public void submitList(List<SearchResultItem> newResults) {
        results.clear();
        results.addAll(newResults);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSearchResultBinding binding = ItemSearchResultBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(results.get(position));
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemSearchResultBinding binding;

        ViewHolder(ItemSearchResultBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(SearchResultItem item) {
            binding.itemTitle.setText(item.getTitle());
            binding.itemSubtitle.setText(item.getSubtitle());
            binding.itemType.setText(SearchResultTypeLabels.labelFor(binding.getRoot().getContext(), item.getType()));
        }
    }
}
