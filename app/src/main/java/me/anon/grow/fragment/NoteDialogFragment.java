package me.anon.grow.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import lombok.Setter;
import me.anon.grow.R;
import me.anon.lib.Views;

@Views.Injectable
public class NoteDialogFragment extends DialogFragment
{
	public static interface OnDialogConfirmed
	{
		public void onDialogConfirmed(String notes);
	}

	@Views.InjectView(R.id.notes) private EditText notes;

	@Setter private OnDialogConfirmed onDialogConfirmed;

	public NoteDialogFragment(){}

	@Override public Dialog onCreateDialog(Bundle savedInstanceState)
	{
		final Context context = getActivity();

		AlertDialog.Builder dialog = new AlertDialog.Builder(context);
		dialog.setTitle("Add note");
		View view = LayoutInflater.from(getActivity()).inflate(R.layout.note_dialog, null);

		Views.inject(this, view);

		dialog.setView(view);
		dialog.setPositiveButton("Add", new DialogInterface.OnClickListener()
		{
			@Override public void onClick(DialogInterface dialog, int which)
			{
				if (onDialogConfirmed != null)
				{
					onDialogConfirmed.onDialogConfirmed(TextUtils.isEmpty(notes.getText()) ? null : notes.getText().toString());
				}
			}
		});
		dialog.setNegativeButton("Cancel", null);

		return dialog.create();
	}
}