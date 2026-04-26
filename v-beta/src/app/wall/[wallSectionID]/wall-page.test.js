import React from "react";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import WallSectionPage from "./page";
import {
  createWallSectionProblem,
  deleteWallSectionProblem,
  fetchWallSectionProblemsForUser,
  fetchWallSectionsForUser,
  resetWallSection,
} from "@/api/wallSections";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { useParams, useRouter } from "next/navigation";
import { toast } from "react-toastify";

jest.mock("@/api/wallSections", () => ({
  createWallSectionProblem: jest.fn(),
  deleteWallSectionProblem: jest.fn(),
  fetchWallSectionProblemsForUser: jest.fn(),
  fetchWallSectionsForUser: jest.fn(),
  resetWallSection: jest.fn(),
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
  ArrowLeftIcon: () => <span>Back icon</span>,
  MoreVertical: () => <span>More</span>,
}));

jest.mock("@/components/GuestBanner", () => ({
  __esModule: true,
  default: ({ message }) => <div data-testid="guest-banner">{message}</div>,
}));

jest.mock("@/components/ui/PageLoader", () => ({
  __esModule: true,
  default: ({ message }) => <div data-testid="page-loader">{message || "Loading..."}</div>,
}));

jest.mock("@/components/ui/button", () => ({
  Button: ({ children, render, ...props }) =>
    render ? React.cloneElement(render, props, children) : <button {...props}>{children}</button>,
}));

jest.mock("@/components/ui/card", () => ({
  Card: ({ children, ...props }) => <div {...props}>{children}</div>,
  CardHeader: ({ children, ...props }) => <div {...props}>{children}</div>,
  CardAction: ({ children, ...props }) => <div {...props}>{children}</div>,
  CardTitle: ({ children, ...props }) => <h2 {...props}>{children}</h2>,
  CardDescription: ({ children, ...props }) => <div {...props}>{children}</div>,
  CardContent: ({ children, ...props }) => <div {...props}>{children}</div>,
  CardFooter: ({ children, ...props }) => <div {...props}>{children}</div>,
}));

jest.mock("@/components/ui/dialog", () => ({
  Dialog: ({ children, open }) => (open ? <div data-testid="add-dialog">{children}</div> : null),
  DialogContent: ({ children, ...props }) => <div {...props}>{children}</div>,
  DialogHeader: ({ children, ...props }) => <div {...props}>{children}</div>,
  DialogTitle: ({ children, ...props }) => <h3 {...props}>{children}</h3>,
  DialogDescription: ({ children, ...props }) => <p {...props}>{children}</p>,
  DialogFooter: ({ children, ...props }) => <div {...props}>{children}</div>,
}));

jest.mock("@/components/ui/alert-dialog", () => {
  const ReactLocal = require("react");
  const AlertDialogContext = ReactLocal.createContext({ onOpenChange: () => {} });

  return {
    AlertDialog: ({ children, open, onOpenChange }) => (
      <AlertDialogContext.Provider value={{ onOpenChange }}>
        {open ? <div data-testid="alert-dialog">{children}</div> : null}
      </AlertDialogContext.Provider>
    ),
    AlertDialogContent: ({ children, ...props }) => <div {...props}>{children}</div>,
    AlertDialogHeader: ({ children, ...props }) => <div {...props}>{children}</div>,
    AlertDialogTitle: ({ children, ...props }) => <h3 {...props}>{children}</h3>,
    AlertDialogDescription: ({ children, ...props }) => <p {...props}>{children}</p>,
    AlertDialogFooter: ({ children, ...props }) => <div {...props}>{children}</div>,
    AlertDialogCancel: ({ children, onClick, ...props }) => {
      const { onOpenChange } = ReactLocal.useContext(AlertDialogContext);
      return (
        <button
          {...props}
          onClick={(event) => {
            onClick?.(event);
            onOpenChange(false);
          }}
        >
          {children}
        </button>
      );
    },
    AlertDialogAction: ({ children, ...props }) => <button {...props}>{children}</button>,
  };
});

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
const mockReplace = jest.fn();
const setterUser = { uid: "setter-1", email: "setter@example.com" };

const section = {
  wallSectionID: 10,
  wallSectionName: "Main Wall",
  wallSectionInfo: "Lead section",
};

const oneProblem = [
  { problemId: 1, holdColor: "Blue", info: "Balance move" },
];

const manyProblems = [
  { problemId: 1, holdColor: "Blue", info: "Balance move" },
  { problemId: 2, holdColor: "Red", info: "Power move" },
];

function renderWall({
  param = "10",
  ready = true,
  user = setterUser,
  account = { roleName: "SETTER" },
  sections = [section],
  problems = oneProblem,
} = {}) {
  useParams.mockReturnValue({ wallSectionID: param });
  useRequireAuth.mockReturnValue({ ready, user, account });
  fetchWallSectionsForUser.mockResolvedValue(sections);
  fetchWallSectionProblemsForUser.mockResolvedValue(problems);
  return render(<WallSectionPage />);
}

describe("WallSectionPage coverage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    useRouter.mockReturnValue({ push: mockPush, replace: mockReplace });
  });

  describe("Zero", () => {
    it("shows loader when auth is not ready", () => {
      renderWall({ ready: false });
      expect(screen.getByTestId("page-loader")).toBeInTheDocument();
      expect(fetchWallSectionsForUser).not.toHaveBeenCalled();
    });

    it("shows invalid wall section id error for non-numeric params", async () => {
      renderWall({ param: "abc" });
      expect(await screen.findByText("Invalid wall section id.")).toBeInTheDocument();
    });

    it("shows empty state when wall has no problems", async () => {
      renderWall({ problems: [] });
      expect(await screen.findByText("No problems found for this wall section.")).toBeInTheDocument();
    });

    it("shows guest banner for unsigned user", async () => {
      renderWall({ user: null, account: null });
      expect(await screen.findByTestId("guest-banner")).toBeInTheDocument();
      expect(fetchWallSectionsForUser).toHaveBeenCalledWith(null);
    });
  });

  describe("One and Many", () => {
    it("renders one problem and navigates to problem details", async () => {
      renderWall({ problems: oneProblem });
      expect(await screen.findByText("Blue")).toBeInTheDocument();
      fireEvent.click(screen.getByRole("button", { name: "View problem" }));
      expect(mockPush).toHaveBeenCalledWith("/wall/10/problem/1");
    });

    it("renders many problems and back navigation", async () => {
      renderWall({ problems: manyProblems });
      expect(await screen.findByText("Blue")).toBeInTheDocument();
      expect(screen.getByText("Red")).toBeInTheDocument();
      fireEvent.click(screen.getByRole("button", { name: "Back icon" }));
      expect(mockPush).toHaveBeenCalledWith("/main-page");
    });
  });

  describe("Boundaries and Interfaces", () => {
    it("redirects if wall section id does not exist", async () => {
      renderWall({ param: "999", sections: [section] });
      await waitFor(() => {
        expect(toast.error).toHaveBeenCalledWith("That wall section does not exist.");
      });
      expect(mockReplace).toHaveBeenCalledWith("/main-page");
    });

    it("calls section and problems APIs with expected arguments", async () => {
      renderWall({ param: "10", user: setterUser });
      await screen.findByText("Blue");
      expect(fetchWallSectionsForUser).toHaveBeenCalledWith(setterUser);
      expect(fetchWallSectionProblemsForUser).toHaveBeenCalledWith(setterUser, 10);
    });

    it("hides management controls when user is not setter", async () => {
      renderWall({ account: { roleName: "CLIMBER" } });
      await screen.findByText("Blue");
      expect(screen.queryByRole("button", { name: "Reset Wall Section" })).not.toBeInTheDocument();
      expect(screen.queryByRole("button", { name: "Add New Problem" })).not.toBeInTheDocument();
      expect(screen.queryByLabelText("Problem actions")).not.toBeInTheDocument();
    });

    it("rejects invalid add problem grade", async () => {
      renderWall();
      await screen.findByText("Blue");
      fireEvent.click(screen.getByRole("button", { name: "Add New Problem" }));
      fireEvent.change(screen.getByLabelText("Hold Color"), { target: { value: "Green" } });
      fireEvent.change(screen.getByLabelText("Assigned Grade"), { target: { value: "V99" } });
      fireEvent.change(screen.getByLabelText("Notes"), { target: { value: "Hard crux" } });
      fireEvent.click(screen.getByRole("button", { name: "Add problem" }));
      expect(createWallSectionProblem).not.toHaveBeenCalled();
      expect(toast.error).toHaveBeenCalledWith("Enter a valid grade: VB or V0 through V17.");
    });

    it("submits add problem with normalized grade and closes dialog", async () => {
      createWallSectionProblem.mockResolvedValue({});
      renderWall();
      await screen.findByText("Blue");
      fireEvent.click(screen.getByRole("button", { name: "Add New Problem" }));
      fireEvent.change(screen.getByLabelText("Hold Color"), { target: { value: "Green" } });
      fireEvent.change(screen.getByLabelText("Assigned Grade"), { target: { value: "v4" } });
      fireEvent.change(screen.getByLabelText("Notes"), { target: { value: "Hard crux" } });
      fireEvent.click(screen.getByRole("button", { name: "Add problem" }));
      await waitFor(() => {
        expect(createWallSectionProblem).toHaveBeenCalledWith(setterUser, 10, {
          holdColor: "Green",
          info: "Hard crux",
          assignedGrade: "V4",
        });
      });
      expect(toast.success).toHaveBeenCalledWith("Problem added.");
    });

    it("disables add submit during pending request", async () => {
      let resolveAdd;
      createWallSectionProblem.mockImplementation(
        () =>
          new Promise((resolve) => {
            resolveAdd = resolve;
          }),
      );
      renderWall();
      await screen.findByText("Blue");
      fireEvent.click(screen.getByRole("button", { name: "Add New Problem" }));
      fireEvent.change(screen.getByLabelText("Hold Color"), { target: { value: "Green" } });
      fireEvent.change(screen.getByLabelText("Assigned Grade"), { target: { value: "V3" } });
      fireEvent.change(screen.getByLabelText("Notes"), { target: { value: "Hard crux" } });
      fireEvent.click(screen.getByRole("button", { name: "Add problem" }));
      expect(screen.getByRole("button", { name: "Adding…" })).toBeDisabled();
      resolveAdd({});
      await waitFor(() => {
        expect(screen.queryByTestId("add-dialog")).not.toBeInTheDocument();
      });
    });
  });

  describe("Exceptions", () => {
    it("surfaces section fetch errors", async () => {
      fetchWallSectionsForUser.mockRejectedValue(new Error("Failed to fetch: 500"));
      useParams.mockReturnValue({ wallSectionID: "10" });
      useRequireAuth.mockReturnValue({ ready: true, user: setterUser, account: { roleName: "SETTER" } });
      render(<WallSectionPage />);
      expect(await screen.findByText("Failed to fetch: 500")).toBeInTheDocument();
    });

    it("shows toast and fallback when problem fetch fails", async () => {
      fetchWallSectionProblemsForUser.mockRejectedValue(new Error("Failed to fetch wall section problems: 500"));
      useParams.mockReturnValue({ wallSectionID: "10" });
      useRequireAuth.mockReturnValue({ ready: true, user: setterUser, account: { roleName: "SETTER" } });
      fetchWallSectionsForUser.mockResolvedValue([section]);
      render(<WallSectionPage />);
      expect(await screen.findByText("No problems found for this wall section.")).toBeInTheDocument();
      expect(toast.error).toHaveBeenCalledWith("Failed to fetch wall section problems: 500");
    });

    it("shows error toast when delete problem fails", async () => {
      deleteWallSectionProblem.mockRejectedValue(new Error("Failed to delete problem: 500"));
      renderWall();
      await screen.findByText("Blue");
      fireEvent.click(screen.getByLabelText("Problem actions"));
      fireEvent.click(screen.getByRole("button", { name: "Delete" }));
      const alert = screen.getByTestId("alert-dialog");
      fireEvent.click(within(alert).getByRole("button", { name: /^Delete$/ }));
      await waitFor(() => {
        expect(toast.error).toHaveBeenCalledWith("Failed to delete problem: 500");
      });
    });

    it("shows error toast when resetting wall section fails", async () => {
      resetWallSection.mockRejectedValue(new Error("Failed to reset wall section: 500"));
      renderWall();
      await screen.findByText("Blue");
      fireEvent.click(screen.getByRole("button", { name: "Reset Wall Section" }));
      const alert = screen.getByTestId("alert-dialog");
      fireEvent.click(within(alert).getByRole("button", { name: /^Reset$/ }));
      await waitFor(() => {
        expect(toast.error).toHaveBeenCalledWith("Failed to reset wall section: 500");
      });
    });
  });
});
