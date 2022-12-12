<?php

use App\Models\SLang;
use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

class AddDataToLangTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('s_lang', function (Blueprint $table) {
            SLang::create([
                'S_Lng_name' => 'Български',
            ]);
            SLang::create([
                'S_Lng_name' => 'English',
            ]);
        });
    }

    /**
     * Reverse the migrations.
     *
     * @return void
     */
    public function down()
    {
        Schema::table('s_lang', function (Blueprint $table) {
            //
        });
    }
}
