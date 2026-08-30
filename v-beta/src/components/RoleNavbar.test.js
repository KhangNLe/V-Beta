import { render, screen } from "@testing-library/react";
import RoleNavbar from "./RoleNavbar";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { usePathname, useRouter } from "next/navigation";

jest.mock("@/components/NotificationBell", () => ({
  __esModule: true,
  default: () => <div data-testid="notification-bell">Bell</div>,
}));

jest.mock("@/hooks/useRequireAuth", () => ({
  useRequireAuth: jest.fn(),
}));

jest.mock("next/navigation", () => ({
  usePathname: jest.fn(),
  useRouter: jest.fn(),
}));

jest.mock("firebase/auth", () => ({
  signOut: jest.fn(),
}));

jest.mock("@/app/firebase", () => ({
  auth: {},
}));

describe("RoleNavbar notifications entry", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    useRouter.mockReturnValue({ push: jest.fn() });
    usePathname.mockReturnValue("/main-page");
    window.localStorage.setItem("theme", "light");
    window.matchMedia = jest.fn().mockReturnValue({
      matches: false,
      addEventListener: jest.fn(),
      removeEventListener: jest.fn(),
    });
  });

  it("hides the navbar on the about page", () => {
    usePathname.mockReturnValue("/about");
    useRequireAuth.mockReturnValue({ ready: true, user: null, account: null });
    const { container } = render(<RoleNavbar />);
    expect(container).toBeEmptyDOMElement();
  });

  it("hides the bell for guests", () => {
    useRequireAuth.mockReturnValue({ ready: true, user: null, account: null });
    render(<RoleNavbar />);
    expect(screen.queryByTestId("notification-bell")).not.toBeInTheDocument();
    expect(screen.getByRole("link", { name: "V-Beta" })).toHaveAttribute("href", "/");
  });

  it("sends signed-in users to the gym from the V-Beta brand", () => {
    useRequireAuth.mockReturnValue({
      ready: true,
      user: { uid: "firebase-uid" },
      account: { id: 5, roleName: "CLIMBER" },
    });
    render(<RoleNavbar />);
    expect(screen.getByRole("link", { name: "V-Beta" })).toHaveAttribute(
      "href",
      "/main-page",
    );
  });

  it("shows the bell for signed-in users", () => {
    useRequireAuth.mockReturnValue({
      ready: true,
      user: { uid: "firebase-uid" },
      account: { id: 5, roleName: "CLIMBER" },
    });
    render(<RoleNavbar />);
    expect(screen.getByTestId("notification-bell")).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Reports" })).not.toBeInTheDocument();
  });

  it("shows Reports for admins", () => {
    useRequireAuth.mockReturnValue({
      ready: true,
      user: { uid: "firebase-uid" },
      account: { id: 1, roleName: "ADMIN" },
    });
    render(<RoleNavbar />);
    expect(screen.getByRole("link", { name: "Reports" })).toHaveAttribute("href", "/reports");
    expect(screen.getByRole("link", { name: "Appeals" })).toHaveAttribute(
      "href",
      "/appeal-queue",
    );
    expect(screen.getByRole("link", { name: "Logbook" })).toHaveAttribute(
      "href",
      "/logbook",
    );
  });

  it("hides Logbook for climbers", () => {
    useRequireAuth.mockReturnValue({
      ready: true,
      user: { uid: "firebase-uid" },
      account: { id: 5, roleName: "CLIMBER" },
    });
    render(<RoleNavbar />);
    expect(screen.queryByRole("link", { name: "Logbook" })).not.toBeInTheDocument();
  });
});
