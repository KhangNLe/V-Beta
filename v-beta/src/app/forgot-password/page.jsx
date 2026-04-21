import { ForgotPasswordForm } from "@/components/ui/forgot-password-form";
import { AuthBrandedShell } from "@/components/auth-branded-shell";
import { GuestRouteGuard } from "@/components/guest-route-guard";

export default function ForgotPasswordPage() {
  return (
    <GuestRouteGuard>
      <AuthBrandedShell>
        <ForgotPasswordForm />
      </AuthBrandedShell>
    </GuestRouteGuard>
  );
}
