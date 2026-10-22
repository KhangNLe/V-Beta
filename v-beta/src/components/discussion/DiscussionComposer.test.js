import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import DiscussionComposer from "./DiscussionComposer";

describe("DiscussionComposer", () => {
  it("submits trimmed text and clears after a successful post", async () => {
    const onSubmit = jest.fn().mockResolvedValue(true);
    render(<DiscussionComposer onSubmit={onSubmit} />);

    fireEvent.change(screen.getByPlaceholderText("Write a comment here!"), {
      target: { value: "  Heel hook  " },
    });
    fireEvent.click(screen.getByRole("button", { name: "Post" }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith({ text: "  Heel hook  ", file: null });
    });
    await waitFor(() => {
      expect(screen.getByPlaceholderText("Write a comment here!")).toHaveValue("");
    });
  });

  it("keeps the draft when submit returns false", async () => {
    const onSubmit = jest.fn().mockResolvedValue(false);
    render(<DiscussionComposer onSubmit={onSubmit} />);

    fireEvent.change(screen.getByPlaceholderText("Write a comment here!"), {
      target: { value: "Keep me" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Post" }));

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalled();
    });
    expect(screen.getByPlaceholderText("Write a comment here!")).toHaveValue("Keep me");
  });

  it("disables submit when over the character limit", () => {
    render(<DiscussionComposer onSubmit={jest.fn()} />);
    fireEvent.change(screen.getByPlaceholderText("Write a comment here!"), {
      target: { value: "a".repeat(251) },
    });
    expect(screen.getByText("251/250")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Post" })).toBeDisabled();
  });

  it("renders an inline reply composer with cancel", () => {
    const onCancel = jest.fn();
    render(
      <DiscussionComposer
        variant="inline"
        heading="Reply to owner"
        placeholder="Write a reply…"
        submitLabel="Reply"
        onSubmit={jest.fn()}
        onCancel={onCancel}
      />,
    );

    expect(screen.getByText("Reply to owner")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Write a reply…")).toBeInTheDocument();
    expect(screen.getByLabelText("Discussion reply")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Reply" })).toBeDisabled();

    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
    expect(onCancel).toHaveBeenCalled();
  });

  it("attaches a video file and allows clearing it", () => {
    render(<DiscussionComposer onSubmit={jest.fn()} />);
    const input = screen.getByLabelText("Choose Video");
    const file = new File(["video"], "beta.mp4", { type: "video/mp4" });

    fireEvent.change(input, { target: { files: [file] } });
    expect(screen.getByText("beta.mp4")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Post" })).not.toBeDisabled();

    fireEvent.click(screen.getByLabelText("Remove video"));
    expect(screen.queryByText("beta.mp4")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Post" })).toBeDisabled();
  });
});
