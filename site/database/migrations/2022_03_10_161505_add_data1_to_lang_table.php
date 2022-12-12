<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

class AddData1ToLangTable extends Migration
{
    /**
     * Run the migrations.
     *
     * @return void
     */
    public function up()
    {
        Schema::table('s_lang', function (Blueprint $table) {
            DB::table('s_lang')->where('S_Lng_id', 1)
                ->update(array('S_Lng_key' => 'bg', 'St_id' => '1'));

            DB::table('s_lang')->where('S_Lng_id', 2)
                ->update(array('S_Lng_key' => 'en'));
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
