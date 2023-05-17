package com.kenneth_bustamante.fruit_inventory_portfolio_app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class FruitAdapter extends RecyclerView.Adapter<FruitAdapter.FruitViewHolder> {
    // Define variables
    private List<Fruit> fruitList;
    OnItemClickListener mOnItemClickListener;

    public FruitAdapter(Context context, List<Fruit> fruitList, OnItemClickListener mOnItemClickListener) {
        this.fruitList = fruitList;
        this.mOnItemClickListener = mOnItemClickListener;
    }

    @NonNull
    @Override
    public FruitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the fruit_row.xml layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fruit_row, parent, false);
        return new FruitViewHolder(view, mOnItemClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull FruitViewHolder holder, int position) {
        // Bind the data to the views in the ViewHolder
        Fruit fruit = fruitList.get(position);
        holder.fruitNameTextView.setText(fruit.getName());
        holder.amountTextView.setText(fruit.getAmount() <= 99 ? String.valueOf(fruit.getAmount()) : "+99");

        if (fruit.getImageUrl() != null && !fruit.getImageUrl().isEmpty()) {
            // Set the fruit image if available
            Picasso.get().load(fruit.getImageUrl()).into(holder.fruitImageView);
        } else {
            // Set a default image if the fruit has no image
            holder.fruitImageView.setImageResource(R.mipmap.fruits_ilustration);
        }
    }

    @Override
    public int getItemCount() {
        // Return the total number of fruit items
        return fruitList.size();
    }

    // ViewHolder class
    public static class FruitViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ImageView fruitImageView;
        private TextView fruitNameTextView;
        private TextView amountTextView;

        OnItemClickListener onItemClickListener;

        public FruitViewHolder(@NonNull View itemView, OnItemClickListener onItemClickListener) {
            super(itemView);
            fruitImageView = itemView.findViewById(R.id.fruitImageIV);
            fruitNameTextView = itemView.findViewById(R.id.fruitNameTV);
            amountTextView = itemView.findViewById(R.id.amountTV);

            this.onItemClickListener = onItemClickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onItemClickListener.onItemClick(getAdapterPosition());
        }
    }

    public interface OnItemClickListener {
        public void onItemClick(int position);
    }
    
}

// Copyright © 2023 Kenneth Bustamante Zuluaga