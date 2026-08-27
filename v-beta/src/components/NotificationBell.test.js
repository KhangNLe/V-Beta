import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import NotificationBell from "./NotificationBell";
import {
  fetchUnreadNotifications,
  markNotificationRead,
} from "@/api/notifications";
import { usePathname, useRouter } from "next/navigation";

jest.mock("@/api/notifications", () => ({
  fetchUnreadNotifications: jest.fn(),
  markNotificationRead: jest.fn(),
}));

jest.mock("next/navigation", () => ({
  usePathname: jest.fn(),
  useRouter: jest.fn(),
}));

jest.mock("lucide-react", () => ({
  Bell: () => <span>BellIcon</span>,
}));

jest.mock("@/components/ui/dropdown-menu", () => {
  const ReactLocal = require("react");
  const MenuContext = ReactLocal.createContext({ open: false, setOpen: () => {} });

  return {
    DropdownMenu: ({ children, onOpenChange }) => {
      const [open, setOpen] = ReactLocal.useState(false);
      const updateOpen = (next) => {
        setOpen(next);
        onOpenChange?.(next);
      };
      return (
        <MenuContext.Provider value={{ open, setOpen: updateOpen }}>
          {children}
        </MenuContext.Provider>
      );
    },
    DropdownMenuTrigger: ({ render, children }) => {
      const { setOpen } = ReactLocal.useContext(MenuContext);
      return ReactLocal.cloneElement(render, {
        ...render.props,
        onClick: () => setOpen(true),
      }, children);
    },
    DropdownMenuContent: ({ children }) => {
      const { open } = ReactLocal.useContext(MenuContext);
      return open ? <div data-testid="notification-dropdown">{children}</div> : null;
    },
    DropdownMenuItem: ({ children, onClick, ...props }) => (
      <button type="button" {...props} onClick={onClick}>
        {children}
      </button>
    ),
    DropdownMenuSeparator: () => <hr />,
  };
});

const mockPush = jest.fn();
const user = { uid: "firebase-uid" };

const reportCreated = {
  notificationId: 81,
  summary: {
    eventTypeName: "REPORT_CREATED",
    description: "A user submitted a content report",
  },
  click: {
    kind: "REPORT_QUEUE",
    reportId: 11,
  },
  createdAt: "2026-08-14T19:11:00Z",
};

const contentRemoved = {
  notificationId: 83,
  summary: {
    eventTypeName: "CONTENT_REMOVED",
    description: "One of your content had been reported and removed.",
  },
  click: {
    kind: "REPORT_QUEUE",
    reportId: 11,
  },
  createdAt: "2026-08-18T18:00:00Z",
};

describe("NotificationBell", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    usePathname.mockReturnValue("/main-page");
    useRouter.mockReturnValue({ push: mockPush });
    fetchUnreadNotifications.mockResolvedValue([]);
    markNotificationRead.mockResolvedValue(undefined);
  });

  it("shows the bell with an unread count", async () => {
    fetchUnreadNotifications.mockResolvedValue([reportCreated, contentRemoved]);
    render(<NotificationBell user={user} />);
    expect(await screen.findByLabelText("Notifications, 2 unread")).toBeInTheDocument();
  });

  it("lists unread items and navigates to the notification target", async () => {
    fetchUnreadNotifications.mockResolvedValue([reportCreated]);
    render(<NotificationBell user={user} />);
    fireEvent.click(await screen.findByLabelText("Notifications, 1 unread"));
    expect(await screen.findByTestId("notification-dropdown")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: /New report/i }));
    await waitFor(() => {
      expect(markNotificationRead).toHaveBeenCalledWith(user, 81);
    });
    expect(mockPush).toHaveBeenCalledWith("/reports?reportId=11");
  });

  it("navigates owner items to appeal context from the dropdown", async () => {
    fetchUnreadNotifications.mockResolvedValue([contentRemoved]);
    render(<NotificationBell user={user} />);
    fireEvent.click(await screen.findByLabelText("Notifications, 1 unread"));
    fireEvent.click(await screen.findByRole("button", { name: /Content removed/i }));
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith("/appeals?reportId=11");
    });
  });

  it("sends Show all notifications to the notifications page", async () => {
    fetchUnreadNotifications.mockResolvedValue([reportCreated]);
    render(<NotificationBell user={user} />);
    fireEvent.click(await screen.findByLabelText("Notifications, 1 unread"));
    fireEvent.click(screen.getByRole("button", { name: "Show all notifications" }));
    expect(mockPush).toHaveBeenCalledWith("/notifications");
    expect(markNotificationRead).not.toHaveBeenCalled();
  });

  it("shows an empty unread state in the dropdown", async () => {
    render(<NotificationBell user={user} />);
    fireEvent.click(await screen.findByLabelText("Notifications"));
    expect(await screen.findByText("No unread notifications.")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Show all notifications" })).toBeInTheDocument();
  });
});
