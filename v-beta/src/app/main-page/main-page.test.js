import React from "react";
import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import MainPage from "./page";
import {
  addWallSection,
  deleteWallSection,
  fetchWallSectionsForUser,
} from "@/api/wallSections";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { useRouter } from "next/navigation";
import { toast } from "react-toastify";

jest.mock("@/api/wallSections", () => ({
  addWallSection: jest.fn(),
  deleteWallSection: jest.fn(),
  fetchWallSectionsForUser: jest.fn(),
}));

jest.mock("@/hooks/useRequireAuth", () => ({
  useRequireAuth: jest.fn(),
}));

jest.mock("next/navigation", () => ({
  useRouter: jest.fn(),
}));

jest.mock("react-toastify", () => ({
  toast: {
    error: jest.fn(),
    success: jest.fn(),
  },
}));

jest.mock("@/components/GuestBanner", () => ({
  __esModule: true,
  default: ({ message }) => <div data-testid="guest-banner">{message}</div>,
}));

jest.mock("@/components/ui/PageLoader", () => ({
  __esModule: true,
  default: () => <div data-testid="page-loader">Loading...</div>,
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
  CardDescription: ({ children, ...props }) => <p {...props}>{children}</p>,
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
  const React = require("react");
  const AlertDialogContext = React.createContext({ onOpenChange: () => {} });

  return {
    AlertDialog: ({ children, open, onOpenChange }) => (
      <AlertDialogContext.Provider value={{ onOpenChange }}>
        {open ? <div data-testid="delete-dialog">{children}</div> : null}
      </AlertDialogContext.Provider>
    ),
    AlertDialogContent: ({ children, ...props }) => <div {...props}>{children}</div>,
    AlertDialogHeader: ({ children, ...props }) => <div {...props}>{children}</div>,
    AlertDialogTitle: ({ children, ...props }) => <h3 {...props}>{children}</h3>,
    AlertDialogDescription: ({ children, ...props }) => <p {...props}>{children}</p>,
    AlertDialogFooter: ({ children, ...props }) => <div {...props}>{children}</div>,
    AlertDialogCancel: ({ children, onClick, ...props }) => {
      const { onOpenChange } = React.useContext(AlertDialogContext);
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
  const React = require("react");
  const MenuContext = React.createContext({ open: false, setOpen: () => {} });

  return {
    DropdownMenu: ({ children }) => {
      const [open, setOpen] = React.useState(false);
      return <MenuContext.Provider value={{ open, setOpen }}>{children}</MenuContext.Provider>;
    },
    DropdownMenuTrigger: ({ render }) => {
      const { setOpen } = React.useContext(MenuContext);
      return React.cloneElement(render, {
        ...render.props,
        onClick: () => setOpen(true),
      });
    },
    DropdownMenuContent: ({ children }) => {
      const { open } = React.useContext(MenuContext);
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
const mockUser = {
  email: "climber@example.com",
  getIdToken: jest.fn().mockResolvedValue("token"),
};

const adminAccount = { roleName: "ADMIN" };
const climberAccount = { roleName: "CLIMBER" };

const oneSection = [
  { wallSectionID: 1, wallSectionName: "Slab", wallSectionInfo: "Technical slab" },
];

const manySections = [
  { wallSectionID: 1, wallSectionName: "Slab", wallSectionInfo: "Technical slab" },
  { wallSectionID: 2, wallSectionName: "Cave", wallSectionInfo: "Steep cave" },
];

function renderMainPage({
  user = mockUser,
  account = adminAccount,
  ready = true,
  sections = [],
} = {}) {
  useRequireAuth.mockReturnValue({ user, account, ready });
  fetchWallSectionsForUser.mockResolvedValue(sections);
  return render(<MainPage />);
}

describe("MainPage coverage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    useRouter.mockReturnValue({ push: mockPush });
  });

  it("shows loader while auth is not ready", () => {
    renderMainPage({ ready: false });
    expect(screen.getByTestId("page-loader")).toBeInTheDocument();
    expect(fetchWallSectionsForUser).not.toHaveBeenCalled();
  });

  it("shows guest banner and hides admin controls for guests", async () => {
    renderMainPage({ user: null, account: null, sections: [] });
    expect(await screen.findByText("No wall sections found.")).toBeInTheDocument();
    expect(screen.getByTestId("guest-banner")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Add Wall Section" })).not.toBeInTheDocument();
    expect(fetchWallSectionsForUser).toHaveBeenCalledWith(null);
  });

  it("renders one section and navigates on view click", async () => {
    renderMainPage({ sections: oneSection });
    expect(await screen.findByText("Slab")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "View section" }));
    expect(mockPush).toHaveBeenCalledWith("/wall/1");
  });

  it("renders many sections and independent actions", async () => {
    renderMainPage({ sections: manySections });
    expect(await screen.findByText("Slab")).toBeInTheDocument();
    expect(screen.getByText("Cave")).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: "View section" })).toHaveLength(2);
  });

  it("renders fallback description when section info is missing", async () => {
    renderMainPage({
      sections: [{ wallSectionID: 5, wallSectionName: "Comp Wall", wallSectionInfo: "" }],
    });
    expect(await screen.findByText("No description available for this section.")).toBeInTheDocument();
  });

  it("surfaces fetch errors in the UI", async () => {
    fetchWallSectionsForUser.mockRejectedValue(new Error("Failed to fetch: 500"));
    useRequireAuth.mockReturnValue({ user: mockUser, account: adminAccount, ready: true });
    render(<MainPage />);
    expect(await screen.findByText("Failed to fetch: 500")).toBeInTheDocument();
  });

  it("hides admin-only actions for non-admin users", async () => {
    renderMainPage({ account: climberAccount, sections: oneSection });
    await screen.findByText("Slab");
    expect(screen.queryByRole("button", { name: "Add Wall Section" })).not.toBeInTheDocument();
    expect(screen.queryByLabelText("Section actions")).not.toBeInTheDocument();
  });

  it("validates add form boundaries and blocks empty trimmed values", async () => {
    renderMainPage({ sections: [] });
    fireEvent.click(await screen.findByRole("button", { name: "Add Wall Section" }));
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "   " } });
    fireEvent.change(screen.getByLabelText("Description"), { target: { value: "   " } });
    fireEvent.click(screen.getByRole("button", { name: "Add section" }));

    expect(addWallSection).not.toHaveBeenCalled();
    expect(toast.error).toHaveBeenCalledWith("Please enter both a name and description.");
  });

  it("submits add form with trimmed values and reloads sections", async () => {
    addWallSection.mockResolvedValue({ ok: true });
    fetchWallSectionsForUser.mockResolvedValue([]);
    renderMainPage({ sections: [] });

    fireEvent.click(await screen.findByRole("button", { name: "Add Wall Section" }));
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "  New Wall  " } });
    fireEvent.change(screen.getByLabelText("Description"), { target: { value: "  New Info  " } });
    fireEvent.click(screen.getByRole("button", { name: "Add section" }));

    await waitFor(() => {
      expect(addWallSection).toHaveBeenCalledWith(mockUser, {
        wallSectionName: "New Wall",
        wallSectionInfo: "New Info",
      });
    });
    expect(fetchWallSectionsForUser).toHaveBeenCalledTimes(2);
    expect(toast.success).toHaveBeenCalledWith("Wall section added.");
    expect(screen.queryByTestId("add-dialog")).not.toBeInTheDocument();
  });

  it("shows pending add state and prevents duplicate submit", async () => {
    let resolveAdd;
    addWallSection.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveAdd = resolve;
        }),
    );
    renderMainPage({ sections: [] });

    fireEvent.click(await screen.findByRole("button", { name: "Add Wall Section" }));
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "New Wall" } });
    fireEvent.change(screen.getByLabelText("Description"), { target: { value: "Info" } });
    fireEvent.click(screen.getByRole("button", { name: "Add section" }));

    expect(screen.getByRole("button", { name: "Adding..." })).toBeDisabled();
    fireEvent.click(screen.getByRole("button", { name: "Adding..." }));
    expect(addWallSection).toHaveBeenCalledTimes(1);

    resolveAdd({ ok: true });
    await waitFor(() => {
      expect(screen.queryByTestId("add-dialog")).not.toBeInTheDocument();
    });
  });

  it("opens and cancels delete dialog from section actions menu", async () => {
    renderMainPage({ sections: oneSection });
    await screen.findByText("Slab");

    fireEvent.click(screen.getByLabelText("Section actions"));
    fireEvent.click(screen.getByRole("button", { name: "Delete" }));

    const deleteDialog = screen.getByTestId("delete-dialog");
    expect(within(deleteDialog).getByText(/Delete "Slab"\?/)).toBeInTheDocument();
    fireEvent.click(within(deleteDialog).getByRole("button", { name: "Cancel" }));
    expect(screen.queryByTestId("delete-dialog")).not.toBeInTheDocument();
  });

  it("deletes section and reloads on confirm", async () => {
    deleteWallSection.mockResolvedValue(undefined);
    renderMainPage({ sections: oneSection });
    await screen.findByText("Slab");

    fireEvent.click(screen.getByLabelText("Section actions"));
    fireEvent.click(screen.getByRole("button", { name: "Delete" }));

    const deleteDialog = screen.getByTestId("delete-dialog");
    fireEvent.click(within(deleteDialog).getByRole("button", { name: /^Delete$/ }));

    await waitFor(() => {
      expect(deleteWallSection).toHaveBeenCalledWith(mockUser, 1);
    });
    expect(fetchWallSectionsForUser).toHaveBeenCalledTimes(2);
    expect(toast.success).toHaveBeenCalledWith("Wall section deleted.");
  });

  it("shows deleting state while delete is pending", async () => {
    let resolveDelete;
    deleteWallSection.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveDelete = resolve;
        }),
    );
    renderMainPage({ sections: oneSection });
    await screen.findByText("Slab");

    fireEvent.click(screen.getByLabelText("Section actions"));
    fireEvent.click(screen.getByRole("button", { name: "Delete" }));
    const deleteDialog = screen.getByTestId("delete-dialog");
    fireEvent.click(within(deleteDialog).getByRole("button", { name: /^Delete$/ }));

    expect(within(deleteDialog).getByRole("button", { name: "Deleting..." })).toBeDisabled();
    resolveDelete(undefined);
    await waitFor(() => {
      expect(screen.queryByTestId("delete-dialog")).not.toBeInTheDocument();
    });
  });

  it("captures current contradictory add UX when API helper reports error without throwing", async () => {
    addWallSection.mockImplementation(async () => {
      toast.error("Failed to add wall section: 500");
      return {};
    });
    renderMainPage({ sections: [] });

    fireEvent.click(await screen.findByRole("button", { name: "Add Wall Section" }));
    fireEvent.change(screen.getByLabelText("Name"), { target: { value: "New Wall" } });
    fireEvent.change(screen.getByLabelText("Description"), { target: { value: "Info" } });
    fireEvent.click(screen.getByRole("button", { name: "Add section" }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("Failed to add wall section: 500");
    });
    expect(toast.success).toHaveBeenCalledWith("Wall section added.");
  });
});
