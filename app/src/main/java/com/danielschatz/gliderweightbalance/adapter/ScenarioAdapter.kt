package com.danielschatz.gliderweightbalance.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.danielschatz.gliderweightbalance.data.model.Scenario
import com.danielschatz.gliderweightbalance.databinding.ItemScenarioListBinding

class ScenarioAdapter(
    private val onItemClicked: (Scenario) -> Unit,
    private val onDeleteClicked: (Scenario) -> Unit,
    private val onUpdateClicked: (Scenario) -> Unit
) : ListAdapter<Scenario, ScenarioAdapter.ScenarioViewHolder>(ScenarioDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScenarioViewHolder {
        val binding = ItemScenarioListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ScenarioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScenarioViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ScenarioViewHolder(private val binding: ItemScenarioListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(scenario: Scenario) {
            binding.textViewScenarioName.text = scenario.name
            binding.root.setOnClickListener { onItemClicked(scenario) }
            binding.buttonDeleteScenario.setOnClickListener { onDeleteClicked(scenario) }
            binding.buttonUpdateScenario.setOnClickListener { onUpdateClicked(scenario) }
        }
    }

    class ScenarioDiffCallback : DiffUtil.ItemCallback<Scenario>() {
        override fun areItemsTheSame(oldItem: Scenario, newItem: Scenario): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Scenario, newItem: Scenario): Boolean = oldItem == newItem
    }
}
