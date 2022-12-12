<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Support\Facades\DB;

/**
 * @property int    $ArF_id
 * @property int    $ArL_id
 * @property int    $created_at
 * @property int    $deleted_at
 * @property int    $updated_at
 * @property string $ArF_desc
 * @property string $ArF_file
 * @property string $ArF_name
 * @property string $ArF_size
 * @property string $ArF_type
 */
class MFiles extends Model
{
    /**
     * The database table used by the model.
     *
     * @var string
     */
    protected $table = 'm_files';

    /**
     * The primary key for the model.
     *
     * @var string
     */
    protected $primaryKey = 'ArF_id';

    /**
     * Attributes that should be mass-assignable.
     *
     * @var array
     */
    protected $fillable = [
        'ArF_desc', 'ArF_file', 'ArF_name', 'ArF_size', 'ArF_type', 'ArL_id', 'MvL_id', 'MnL_id', 'ArF_date', 'created_at', 'deleted_at', 'updated_at'
    ];

    /**
     * The attributes excluded from the model's JSON form.
     *
     * @var array
     */
    protected $hidden = [];

    /**
     * The attributes that should be casted to native types.
     *
     * @var array
     */
    protected $casts = [
        'ArF_id' => 'int', 'ArF_desc' => 'string', 'ArF_file' => 'string', 'ArF_name' => 'string', 'ArF_size' => 'string', 'ArF_type' => 'string', 'ArL_id' => 'int', 'MvL_id' => 'int', 'MnL_id' => 'int', 'ArF_date' => 'date', 'created_at' => 'timestamp', 'deleted_at' => 'timestamp', 'updated_at' => 'timestamp'
    ];

    /**
     * The attributes that should be mutated to dates.
     *
     * @var array
     */
    protected $dates = [
        'ArF_date', 'created_at', 'deleted_at', 'updated_at'
    ];

    /**
     * Indicates if the model should be timestamped.
     *
     * @var boolean
     */
    public $timestamps = false;

    public static function boot()
    {
        parent::boot();

        static::creating(function ($article) {
            // ArF_type


            $article->ArF_type = fileAttType($article->ArF_file);
            $article->created_at = now();
            $article->updated_at = now();
        });

        static::saving(function ($article) {
            $article->ArF_type = fileAttType($article->ArF_file);
            $article->updated_at = now();
        });
    }

    // Scopes...

    // Functions ...

    public static function fileList($key, $keyId)
    {
        return DB::table('m_files as r')
            ->where('r.' . $key, $keyId)

            ->whereNull('r.deleted_at')
            ->select(
                'r.*',
                'ArF_desc',
                DB::raw("IF(SUBSTR(ArF_file, 1,4)='pub/',CONCAT('/',ArF_file),CONCAT('/pub/files/',ArF_file))  as ArF_file"),
            )
            ->orderBy('ArF_date', 'desc')
            ->orderBy('ArF_id', 'desc')
            ->get();
    }

    // Relations ...
    public function eq_article()
    {
        return $this->belongsTo(MArticleLng::class, 'ArL_id');
    }

    public function eq_files_news()
    {
        return $this->belongsTo(MNewsLng::class, 'MnL_id');
    }
    public function eq_files_event()
    {
        return $this->belongsTo(MEventLng::class, 'MvL_id');
    }
}
