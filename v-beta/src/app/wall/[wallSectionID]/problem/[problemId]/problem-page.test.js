import React from "react";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import ProblemPage from "./page";
import { fetchProblemForUser } from "@/api/wallSections";
import { addUserSuggestedGrade, deleteUserComment, postCommentForUser } from "@/api/comments";
import {
  deleteSolutionBetaFromDatabase,
  requestSignedUploadUrl,
  saveSolutionBetaToDatabase,
  uploadSolutionBeta,
} from "@/api/solutionBeta";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { useParams, useRouter } from "next/navigation";
import { toast } from "react-toastify";

jest.mock("@/api/wallSections", () => ({
  fetchProblemForUser: jest.fn(),
}));

jest.mock("@/api/comments", () => ({
  addUserSuggestedGrade: jest.fn(),
  deleteUserComment: jest.fn(),
  postCommentForUser: jest.fn(),
}));

jest.mock("@/api/solutionBeta", () => ({
  deleteSolutionBetaFromDatabase: jest.fn(),
  requestSignedUploadUrl: jest.fn(),
  saveSolutionBetaToDatabase: jest.fn(),
  uploadSolutionBeta: jest.fn(),
}));

jest.mock("@/hooks/useRequireAuth", () => ({
  useRequireAuth: jest.fn(),
}));

jest.mock("next/navigation", () => ({
  useParams: jest.fn(),
  useRouter: jest.fn(),
}));

jest.mock("react-toastify", () => ({
  toast: {
    error: jest.fn(),
    success: jest.fn(),
  },
}));

jest.mock("lucide-react", () => ({
  ArrowLeftIcon: () => <span>BackIcon</span>,
  MoreVertical: () => <span>MoreIcon</span>,
}));

jest.mock("@/components/ui/PageLoader", () => ({
  __esModule: true,
  default: ({ message }) => <div data-testid="page-loader">{message || "Loading..."}</div>,
}));

jest.mock("@/components/GuestBanner", () => ({
  __esModule: true,
  default: ({ message }) => <div data-testid="guest-banner">{message}</div>,
}));

jest.mock("@/components/ui/button", () => ({
  Button: ({ children, render, ...props }) =>
    render ? React.cloneElement(render, props, children) : <button {...props}>{children}</button>,
}));

jest.mock("@/components/ui/dropdown-menu", () => {
  const ReactLocal = require("react");
  const MenuContext = ReactLocal.createContext({ open: false, setOpen: () => {} });

  return {
    DropdownMenu: ({ children }) => {
      const [open, setOpen] = ReactLocal.useState(false);
      return <MenuContext.Provider value={{ open, setOpen }}>{children}</MenuContext.Provider>;
    },
    DropdownMenuTrigger: ({ render }) => {
      const { setOpen } = ReactLocal.useContext(MenuContext);
      return ReactLocal.cloneElement(render, {
        ...render.props,
        onClick: () => setOpen(true),
      });
    },
    DropdownMenuContent: ({ children }) => {
      const { open } = ReactLocal.useContext(MenuContext);
      return open ? <div>{children}</div> : null;
    },
    DropdownMenuItem: ({ children, onClick, ...props }) => (
      <button {...props} onClick={onClick}>
        {children}
      </button>
    ),
  };
});

const mockPush = jest.fn();
const user = { uid: "firebase-uid", email: "tester@example.com" };
const ownerAccount = { id: 5, roleName: "CLIMBER" };
const adminAccount = { id: 1, roleName: "ADMIN" };

const baseProblem = {
  holdColor: "Blue",
  assignedGrade: "V4",
  perceiveGrade: "V3",
  info: "Crimp to gaston",
  discussion: [],
};

const commentByOwner = {
  discussionId: 301,
  userId: 5,
  discussionType: "COMMENT",
  discussionContent: "Try heel hook",
  username: "owner",
  createdDate: "2026-04-26T12:00:00.000Z",
  comment: "Try heel hook",
};

const betaByOwner = {
  discussionId: 302,
  userId: 5,
  discussionType: "BETA",
  discussionContent: "https://example.com/beta.mp4",
  username: "owner",
  createdDate: "2026-04-26T12:01:00.000Z",
  comment: null,
  videoURL: "https://example.com/beta.mp4",
};

function renderProblemPage({
  ready = true,
  authUser = user,
  account = ownerAccount,
  wallSectionID = "10",
  problemId = "100",
  problem = baseProblem,
} = {}) {
  useParams.mockReturnValue({ wallSectionID, problemId });
  useRequireAuth.mockReturnValue({ ready, user: authUser, account });
  fetchProblemForUser.mockResolvedValue(problem);
  return render(<ProblemPage />);
}

describe("ProblemPage coverage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    useRouter.mockReturnValue({ push: mockPush });
  });

  describe("Zero", () => {
    it("shows loader while auth is not ready", () => {
      renderProblemPage({ ready: false });
      expect(screen.getByTestId("page-loader")).toBeInTheDocument();
      expect(fetchProblemForUser).not.toHaveBeenCalled();
    });

    it("shows invalid-id error when params are invalid", async () => {
      renderProblemPage({ wallSectionID: "abc", problemId: "100" });
      expect(await screen.findByText("Invalid wall section or problem id.")).toBeInTheDocument();
    });

    it("shows empty discussion state", async () => {
      renderProblemPage({ problem: { ...baseProblem, discussion: [] } });
      expect(await screen.findByText("No comments yet. Be the first to discuss this problem!")).toBeInTheDocument();
    });

    it("shows guest banner and disabled signed-in controls for guest", async () => {
      renderProblemPage({ authUser: null, account: null });
      expect(await screen.findByTestId("guest-banner")).toBeInTheDocument();
      expect(screen.getByRole("button", { name: "Suggest Grade" })).toBeDisabled();
      expect(screen.getByRole("button", { name: "Post Comment" })).toBeDisabled();
    });
  });

  describe("One and Many", () => {
    it("renders one problem payload fields and supports back navigation", async () => {
      renderProblemPage({ problem: { ...baseProblem, discussion: [commentByOwner] } });
      expect(await screen.findByText("Blue")).toBeInTheDocument();
      expect(screen.getByText("Assigned Grade: V4")).toBeInTheDocument();
      fireEvent.click(screen.getByLabelText("Back to wall section"));
      expect(mockPush).toHaveBeenCalledWith("/wall/10");
    });

    it("renders multiple discussion items independently", async () => {
      renderProblemPage({ problem: { ...baseProblem, discussion: [commentByOwner, betaByOwner] } });
      expect(await screen.findByText("Try heel hook")).toBeInTheDocument();
      expect(screen.getByText("Watch Beta")).toBeInTheDocument();
      const actionButtons = screen.getAllByLabelText("Comment actions");
      fireEvent.click(actionButtons[0]);
      expect(screen.getByText("Delete Comment")).toBeInTheDocument();
      fireEvent.click(actionButtons[1]);
      expect(screen.getByText("Delete Solution Beta")).toBeInTheDocument();
    });

    it("posts one comment and refreshes problem", async () => {
      postCommentForUser.mockResolvedValue(undefined);
      fetchProblemForUser
        .mockResolvedValueOnce({ ...baseProblem, discussion: [] })
        .mockResolvedValueOnce({ ...baseProblem, discussion: [commentByOwner] });
      renderProblemPage();
      await screen.findByText("Blue");

      fireEvent.change(screen.getByPlaceholderText("Write a comment here!"), {
        target: { value: "Try heel hook" },
      });
      fireEvent.click(screen.getByRole("button", { name: "Post Comment" }));

      await waitFor(() => {
        expect(postCommentForUser).toHaveBeenCalledWith(user, 100, "Try heel hook");
      });
      expect(await screen.findByText("Try heel hook")).toBeInTheDocument();
    });
  });

  describe("Boundaries and Interfaces", () => {
    it("blocks posting comments over 250 characters", async () => {
      renderProblemPage();
      await screen.findByText("Blue");
      fireEvent.change(screen.getByPlaceholderText("Write a comment here!"), {
        target: { value: "a".repeat(251) },
      });
      expect(screen.getByText("251/250")).toBeInTheDocument();
      expect(screen.getByRole("button", { name: "Post Comment" })).toBeDisabled();
      expect(postCommentForUser).not.toHaveBeenCalled();
    });

    it("allows 250 character comments", async () => {
      renderProblemPage();
      await screen.findByText("Blue");
      fireEvent.change(screen.getByPlaceholderText("Write a comment here!"), {
        target: { value: "a".repeat(250) },
      });
      expect(screen.getByText("250/250")).toBeInTheDocument();
      expect(screen.getByRole("button", { name: "Post Comment" })).not.toBeDisabled();
    });

    it("calls suggest-grade API with route problem id and selected grade", async () => {
      addUserSuggestedGrade.mockResolvedValue(undefined);
      renderProblemPage();
      await screen.findByText("Blue");
      fireEvent.change(screen.getByDisplayValue("VB"), { target: { value: "V5" } });
      fireEvent.click(screen.getByRole("button", { name: "Suggest Grade" }));
      await waitFor(() => {
        expect(addUserSuggestedGrade).toHaveBeenCalledWith(user, { perceivedGrade: "V5" }, 100);
      });
    });

    it("shows delete actions only for owner/admin", async () => {
      renderProblemPage({
        account: { id: 999, roleName: "CLIMBER" },
        problem: { ...baseProblem, discussion: [commentByOwner] },
      });
      expect(await screen.findByText("Try heel hook")).toBeInTheDocument();
      expect(screen.queryByLabelText("Comment actions")).not.toBeInTheDocument();
    });

    it("uses correct payload for owner comment deletion", async () => {
      deleteUserComment.mockResolvedValue(undefined);
      fetchProblemForUser
        .mockResolvedValueOnce({ ...baseProblem, discussion: [commentByOwner] })
        .mockResolvedValueOnce({ ...baseProblem, discussion: [] });
      renderProblemPage({ account: ownerAccount, problem: { ...baseProblem, discussion: [commentByOwner] } });
      await screen.findByText("Try heel hook");
      fireEvent.click(screen.getByLabelText("Comment actions"));
      fireEvent.click(screen.getByRole("button", { name: "Delete Comment" }));
      await waitFor(() => {
        expect(deleteUserComment).toHaveBeenCalledWith(user, {
          authorId: 5,
          problemId: 100,
          discussionId: 301,
          commentContent: "Try heel hook",
        });
      });
    });

    it("uploads beta successfully and saves metadata", async () => {
      requestSignedUploadUrl.mockResolvedValue({
        signedURL: "https://upload.example.com",
        uploadObjectName: "obj-name",
        publicURL: "https://cdn.example.com/beta.mp4",
      });
      uploadSolutionBeta.mockResolvedValue({ ok: true });
      saveSolutionBetaToDatabase.mockResolvedValue(null);
      fetchProblemForUser
        .mockResolvedValueOnce(baseProblem)
        .mockResolvedValueOnce({ ...baseProblem, discussion: [betaByOwner] });
      const headSpy = jest.spyOn(global, "fetch").mockResolvedValue({ ok: true });

      renderProblemPage();
      await screen.findByText("Blue");
      fireEvent.click(screen.getByRole("button", { name: "Submit Beta" }));
      const input = screen.getByLabelText("Choose Video");
      const file = new File(["video"], "beta.mp4", { type: "video/mp4" });
      fireEvent.change(input, { target: { files: [file] } });

      fireEvent.click(screen.getByRole("button", { name: "Upload Solution Beta" }));
      await waitFor(() => {
        expect(requestSignedUploadUrl).toHaveBeenCalled();
      });
      expect(saveSolutionBetaToDatabase).toHaveBeenCalledWith(user, {
        problemId: 100,
        objectFileName: "obj-name",
        videoURL: "https://cdn.example.com/beta.mp4",
      });
      expect(await screen.findByText(/Uploaded and verified from bucket\./)).toBeInTheDocument();
      headSpy.mockRestore();
    });
  });

  describe("Exceptions", () => {
    it("shows fetch error state when initial problem load fails", async () => {
      fetchProblemForUser.mockRejectedValue(new Error("Failed to fetch problem: 500"));
      useParams.mockReturnValue({ wallSectionID: "10", problemId: "100" });
      useRequireAuth.mockReturnValue({ ready: true, user, account: ownerAccount });
      render(<ProblemPage />);
      expect(await screen.findByText("Failed to fetch problem: 500")).toBeInTheDocument();
    });

    it("shows failure when posting comment fails", async () => {
      postCommentForUser.mockRejectedValue(new Error("Failed to post comment: 500"));
      renderProblemPage();
      await screen.findByText("Blue");
      fireEvent.change(screen.getByPlaceholderText("Write a comment here!"), {
        target: { value: "Try heel hook" },
      });
      fireEvent.click(screen.getByRole("button", { name: "Post Comment" }));
      expect(await screen.findByText("Failed to post comment: 500")).toBeInTheDocument();
    });

    it("shows payload error when comment author cannot be parsed for deletion", async () => {
      renderProblemPage({
        account: adminAccount,
        problem: {
          ...baseProblem,
          discussion: [
            {
              ...commentByOwner,
              authorId: "not-a-number",
              userId: "not-a-number",
            },
          ],
        },
      });
      await screen.findByText("Try heel hook");
      fireEvent.click(screen.getByLabelText("Comment actions"));
      fireEvent.click(screen.getByRole("button", { name: "Delete Comment" }));
      expect(toast.error).toHaveBeenCalledWith("Unable to determine comment payload for deletion.");
    });

    it("reports partial upload success when DB save fails", async () => {
      requestSignedUploadUrl.mockResolvedValue({
        signedURL: "https://upload.example.com",
        uploadObjectName: "obj-name",
        publicURL: "https://cdn.example.com/beta.mp4",
      });
      uploadSolutionBeta.mockResolvedValue({ ok: true });
      saveSolutionBetaToDatabase.mockRejectedValue(new Error("db write failed"));
      fetchProblemForUser.mockResolvedValue(baseProblem);
      const headSpy = jest.spyOn(global, "fetch").mockResolvedValue({ ok: true });

      renderProblemPage();
      await screen.findByText("Blue");
      fireEvent.click(screen.getByRole("button", { name: "Submit Beta" }));
      const input = screen.getByLabelText("Choose Video");
      const file = new File(["video"], "beta.mp4", { type: "video/mp4" });
      fireEvent.change(input, { target: { files: [file] } });

      fireEvent.click(screen.getByRole("button", { name: "Upload Solution Beta" }));
      expect(await screen.findByText(/Upload succeeded, but DB save failed: db write failed/)).toBeInTheDocument();
      headSpy.mockRestore();
    });

    it("reports upload failure before completion", async () => {
      requestSignedUploadUrl.mockRejectedValue(new Error("signed-url failed"));
      renderProblemPage();
      await screen.findByText("Blue");
      fireEvent.click(screen.getByRole("button", { name: "Submit Beta" }));
      const input = screen.getByLabelText("Choose Video");
      const file = new File(["video"], "beta.mp4", { type: "video/mp4" });
      fireEvent.change(input, { target: { files: [file] } });
      fireEvent.click(screen.getByRole("button", { name: "Upload Solution Beta" }));
      expect(await screen.findByText(/Upload failed before completion: signed-url failed/)).toBeInTheDocument();
    });
  });
});
