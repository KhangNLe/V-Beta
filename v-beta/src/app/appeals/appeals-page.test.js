import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import AppealsPage from "./page";
import { createAppeal, fetchDeletionNotice } from "@/api/appeals";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { useSearchParams } from "next/navigation";
import { toast } from "react-toastify";

jest.mock("@/api/appeals", () => ({
  fetchDeletionNotice: jest.fn(),
  createAppeal: jest.fn(),
  APPEAL_REASON_MAX_LENGTH: 250,
}));

jest.mock("@/hooks/useRequireAuth", () => ({
  useRequireAuth: jest.fn(),
}));

jest.mock("next/navigation", () => ({
  useSearchParams: jest.fn(),
}));

jest.mock("react-toastify", () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn(),
  },
}));

jest.mock("@/components/ui/PageLoader", () => ({
  __esModule: true,
  default: ({ message }) => <div data-testid="page-loader">{message || "Loading..."}</div>,
}));

jest.mock("@/components/ui/button", () => ({
  Button: ({ children, ...props }) => <button {...props}>{children}</button>,
}));

jest.mock("@/components/ui/card", () => ({
  Card: ({ children, ...props }) => <div {...props}>{children}</div>,
  CardHeader: ({ children, ...props }) => <div {...props}>{children}</div>,
  CardTitle: ({ children, ...props }) => <h2 {...props}>{children}</h2>,
  CardDescription: ({ children, ...props }) => <p {...props}>{children}</p>,
  CardContent: ({ children, ...props }) => <div {...props}>{children}</div>,
}));

const user = { uid: "firebase-uid", email: "owner@example.com" };

const openNotice = {
  reportId: 11,
  reportStatus: "CONTENT_REMOVED",
  adminReason: "Does not belong on this wall.",
  canAppeal: true,
  appealStatus: null,
  report: {
    discussion: {
      discussionType: "COMMENT",
      discussionContent: "hello",
    },
    reporters: [
      {
        reportId: 11,
        reporter: { username: "sam", email: "sam@example.com" },
        categoryName: "SPAM",
        reportReason: "Spammy",
      },
    ],
  },
};

function renderAppeals({
  ready = true,
  currentUser = user,
  reportId = "11",
  notice = openNotice,
} = {}) {
  useRequireAuth.mockReturnValue({ ready, user: currentUser, account: { roleName: "CLIMBER" } });
  useSearchParams.mockReturnValue({ get: () => reportId });
  fetchDeletionNotice.mockResolvedValue(notice);
  return render(<AppealsPage />);
}

describe("AppealsPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    useSearchParams.mockReturnValue({ get: () => "11" });
  });

  it("shows loader while auth is not ready", () => {
    renderAppeals({ ready: false });
    expect(screen.getByTestId("page-loader")).toBeInTheDocument();
    expect(fetchDeletionNotice).not.toHaveBeenCalled();
  });

  it("shows deletion reason, content summary, and report details without the reporter", async () => {
    renderAppeals();
    expect(await screen.findByText("Does not belong on this wall.")).toBeInTheDocument();
    expect(screen.getByText("hello")).toBeInTheDocument();
    expect(screen.getByText("Spam")).toBeInTheDocument();
    expect(screen.getByText("Spammy")).toBeInTheDocument();
    expect(screen.queryByText("sam")).not.toBeInTheDocument();
    expect(screen.queryByText("sam@example.com")).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Submit appeal" })).toBeDisabled();
  });

  it("submits one appeal and then hides the form", async () => {
    createAppeal.mockResolvedValue(undefined);
    renderAppeals();
    fireEvent.change(await screen.findByLabelText("Appeal reason"), {
      target: { value: "This was a joke, please restore." },
    });
    fireEvent.click(screen.getByRole("button", { name: "Submit appeal" }));
    await waitFor(() => {
      expect(createAppeal).toHaveBeenCalledWith(user, {
        reportId: 11,
        appealReason: "This was a joke, please restore.",
      });
    });
    expect(await screen.findByText("Appeal pending review.")).toBeInTheDocument();
    expect(screen.queryByLabelText("Appeal reason")).not.toBeInTheDocument();
    expect(screen.getByText("The appeal form is closed for this report.")).toBeInTheDocument();
  });

  it("blocks a second submit when the API says an appeal already exists", async () => {
    createAppeal.mockRejectedValue(new Error("Appeal already exists"));
    renderAppeals();
    fireEvent.change(await screen.findByLabelText("Appeal reason"), {
      target: { value: "Please restore." },
    });
    fireEvent.click(screen.getByRole("button", { name: "Submit appeal" }));
    await waitFor(() => {
      expect(createAppeal).toHaveBeenCalledTimes(1);
    });
    expect(screen.queryByLabelText("Appeal reason")).not.toBeInTheDocument();
    expect(toast.error).toHaveBeenCalledWith(
      "An appeal was already submitted for this removal.",
    );
  });

  it("shows decided status and keeps the form closed", async () => {
    renderAppeals({
      notice: {
        ...openNotice,
        canAppeal: false,
        appealStatus: "DENIED",
        reportStatus: "APPEAL_DENIED",
      },
    });
    expect(
      await screen.findByText("Appeal denied. This content stays removed."),
    ).toBeInTheDocument();
    expect(screen.queryByLabelText("Appeal reason")).not.toBeInTheDocument();
    expect(createAppeal).not.toHaveBeenCalled();
  });

  it("asks the owner to open a notification when reportId is missing", async () => {
    renderAppeals({ reportId: "" });
    expect(
      await screen.findByText(/Open this page from a content-removed notification/),
    ).toBeInTheDocument();
    expect(fetchDeletionNotice).not.toHaveBeenCalled();
  });
});
