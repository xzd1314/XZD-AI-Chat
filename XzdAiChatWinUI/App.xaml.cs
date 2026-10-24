using Microsoft.UI.Xaml;
using XzdAiChatWinUI.data;

namespace XzdAiChatWinUI;

public partial class App : Application
{
    public static Window? MainWindow { get; private set; }
    public static Store GlobalStore { get; private set; } = null!;

    public App()
    {
        this.InitializeComponent();
        GlobalStore = new Store();
    }

    protected override void OnLaunched(Microsoft.UI.Xaml.LaunchActivatedEventArgs args)
    {
        MainWindow = new MainWindow();
        MainWindow.Activate();
    }
}
